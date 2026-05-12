package com.example.storeservice.service;

import com.example.storeservice.client.WalletInternalClient;
import com.example.storeservice.client.dto.*;
import com.example.storeservice.dto.request.AuctionRequest;
import com.example.storeservice.dto.request.PlaceBidRequest;
import com.example.storeservice.dto.response.AuctionEvent;
import com.example.storeservice.dto.response.AuctionResponse;
import com.example.storeservice.dto.response.BidResponse;
import com.example.storeservice.exception.customException.*;
import com.example.storeservice.mapper.AuctionMapper;
import com.example.storeservice.model.entity.AuctionItem;
import com.example.storeservice.model.entity.Bid;
import com.example.storeservice.model.enums.AuctionStatus;
import com.example.storeservice.model.enums.BidStatus;
import com.example.storeservice.repository.AuctionItemRepository;
import com.example.storeservice.repository.BidRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClientException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuctionServiceImpl implements AuctionService {

    private final AuctionItemRepository auctionRepository;
    private final BidRepository bidRepository;
    private final WalletInternalClient walletClient;
    private final AuctionMapper mapper;
    private final AuctionEventBroadcaster broadcaster;

    /** Self-reference (via Spring proxy) so finalizeOne() runs in its own transaction. */
    @Autowired
    @Lazy
    private AuctionService self;

    @Value("${mscms.store.minimum-bid-increment:1.00}")
    private BigDecimal defaultMinIncrement;

    // -------- CRUD --------

    @Override
    @Transactional
    public AuctionResponse create(AuctionRequest req) {
        if (!req.getAuctionEndTime().isAfter(req.getAuctionStartTime())) {
            throw new InvalidOperationException("auctionEndTime must be after auctionStartTime");
        }
        AuctionItem item = mapper.toEntity(req);
        if (item.getMinimumBidIncrement() == null) item.setMinimumBidIncrement(defaultMinIncrement);
        item.setStatus(req.getAuctionStartTime().isAfter(LocalDateTime.now())
                ? AuctionStatus.SCHEDULED : AuctionStatus.ACTIVE);
        return mapper.toResponse(auctionRepository.save(item));
    }

    @Override
    @Transactional(readOnly = true)
    public AuctionResponse get(Long id) {
        return auctionRepository.findById(id)
                .map(mapper::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Auction", "id", id));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AuctionResponse> list(AuctionStatus status, Pageable pageable) {
        return (status == null
                ? auctionRepository.findAll(pageable)
                : auctionRepository.findByStatus(status, pageable))
                .map(mapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<BidResponse> listBids(Long auctionId, Pageable pageable) {
        return bidRepository.findByAuctionIdOrderByAmountDesc(auctionId, pageable)
                .map(mapper::toResponse);
    }

    // -------- BIDDING --------

    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public BidResponse placeBid(String userKeycloakId, Long auctionId, PlaceBidRequest req) {

        // Lock the auction so concurrent bids serialize
        AuctionItem auction = auctionRepository.lockById(auctionId)
                .orElseThrow(() -> new ResourceNotFoundException("Auction", "id", auctionId));

        LocalDateTime now = LocalDateTime.now();
        if (auction.getStatus() == AuctionStatus.SCHEDULED && !now.isBefore(auction.getAuctionStartTime())) {
            auction.setStatus(AuctionStatus.ACTIVE);  // late-activation fallback
        }
        if (auction.getStatus() != AuctionStatus.ACTIVE) {
            throw new InvalidOperationException("Auction is not active: status=" + auction.getStatus());
        }
        if (now.isAfter(auction.getAuctionEndTime())) {
            throw new InvalidOperationException("Auction has already ended");
        }

        // Validate amount > current + min increment (or >= startingPrice if no bids)
        BigDecimal minRequired = auction.getCurrentHighestBid() != null
                ? auction.getCurrentHighestBid().add(auction.getMinimumBidIncrement())
                : auction.getStartingPrice();
        if (req.getAmount().compareTo(minRequired) < 0) {
            throw new InvalidOperationException("Bid must be at least " + minRequired);
        }
        if (userKeycloakId.equals(auction.getCurrentHighestBidder())) {
            throw new InvalidOperationException("You are already the highest bidder");
        }

        String idempotencyKey = "bid-reserve-" + auctionId + "-" + userKeycloakId + "-" + UUID.randomUUID();

        // 1) Reserve the new amount on this user's wallet
        ReserveRequest reserveReq = ReserveRequest.builder()
                .userKeycloakId(userKeycloakId)
                .amount(req.getAmount())
                .referenceId(String.valueOf(auctionId))
                .referenceType("AUCTION")
                .idempotencyKey(idempotencyKey)
                .description("Bid on auction #" + auctionId)
                .build();

        WalletApiResponse<WalletReservationDto> reserveResp;
        try {
            reserveResp = walletClient.reserve(reserveReq);
        } catch (RestClientException e) {
            log.error("wallet reserve call failed", e);
            throw new WalletOperationException("Wallet service unavailable: " + e.getMessage(), e);
        }
        if (reserveResp == null || !reserveResp.isSuccess() || reserveResp.getData() == null) {
            String msg = reserveResp != null ? reserveResp.getMessage() : "null response from wallet";
            throw new WalletOperationException("Could not reserve funds: " + msg);
        }
        Long newReservationId = reserveResp.getData().getId();

        Bid bid;
        try {
            // 2) Record bid as ACTIVE
            bid = bidRepository.save(Bid.builder()
                    .auctionId(auctionId)
                    .bidderKeycloakId(userKeycloakId)
                    .amount(req.getAmount())
                    .status(BidStatus.ACTIVE)
                    .walletReservationId(newReservationId)
                    .build());

            // 3) Mark previous ACTIVE bid OUTBID and release its reservation
            Long previousHighestBidId = auction.getCurrentHighestBidId();
            if (previousHighestBidId != null) {
                Bid prev = bidRepository.findById(previousHighestBidId).orElse(null);
                if (prev != null && prev.getStatus() == BidStatus.ACTIVE) {
                    prev.setStatus(BidStatus.OUTBID);
                    bidRepository.save(prev);
                    if (prev.getWalletReservationId() != null) {
                        safeReleaseReservation(prev.getWalletReservationId(),
                                "Outbid on auction #" + auctionId + " by bid " + bid.getId());
                    }
                }
            }

            // 4) Update auction highest pointers
            auction.setCurrentHighestBid(bid.getAmount());
            auction.setCurrentHighestBidder(bid.getBidderKeycloakId());
            auction.setCurrentHighestBidId(bid.getId());
            auctionRepository.save(auction);
        } catch (RuntimeException ex) {
            // Compensate: our local DB will rollback via @Transactional, but the wallet
            // reservation is in another service. Best-effort release.
            log.error("Bid post-reserve work failed; compensating release of reservation {}", newReservationId, ex);
            safeReleaseReservation(newReservationId, "Compensating release after bid failure");
            throw ex;
        }

        // 5) Broadcast SSE (after commit would be safer but acceptable here since failure
        // upstream of this point would have thrown and rolled back already)
        broadcaster.publish(auctionId, AuctionEvent.builder()
                .type("bid.placed")
                .auctionId(auctionId)
                .currentHighestBid(bid.getAmount())
                .currentHighestBidder(bid.getBidderKeycloakId())
                .bidId(bid.getId())
                .timestamp(LocalDateTime.now())
                .build());

        log.info("bid placed auction={} bidder={} amount={}", auctionId, userKeycloakId, bid.getAmount());
        return mapper.toResponse(bid);
    }

    @Override
    @Transactional
    public void cancelAuction(Long auctionId) {
        AuctionItem auction = auctionRepository.lockById(auctionId)
                .orElseThrow(() -> new ResourceNotFoundException("Auction", "id", auctionId));
        if (auction.getStatus() == AuctionStatus.ENDED) {
            throw new InvalidOperationException("Cannot cancel ENDED auction");
        }
        auction.setStatus(AuctionStatus.CANCELLED);
        auctionRepository.save(auction);

        // Release every ACTIVE reservation (typically only one)
        List<Bid> activeBids = bidRepository.findByAuctionIdAndStatusInOrderByAmountDesc(
                auctionId, List.of(BidStatus.ACTIVE));
        for (Bid b : activeBids) {
            if (b.getWalletReservationId() != null) {
                safeReleaseReservation(b.getWalletReservationId(),
                        "Auction #" + auctionId + " cancelled");
            }
            b.setStatus(BidStatus.REFUNDED);
            bidRepository.save(b);
        }
        broadcaster.publish(auctionId, AuctionEvent.builder()
                .type("auction.cancelled").auctionId(auctionId)
                .timestamp(LocalDateTime.now()).build());
    }

    // -------- SCHEDULED FINALIZER --------

    @Override
    @Transactional
    public void activateScheduledAuctions() {
        LocalDateTime now = LocalDateTime.now();
        List<AuctionItem> ready = auctionRepository.findReadyToStart(AuctionStatus.SCHEDULED, now);
        for (AuctionItem a : ready) {
            a.setStatus(AuctionStatus.ACTIVE);
            auctionRepository.save(a);
            log.info("auction activated id={}", a.getId());
        }
    }

    @Override
    public void finalizeEndedAuctions() {
        LocalDateTime now = LocalDateTime.now();
        List<Long> endedIds;
        try {
            endedIds = auctionRepository.findEndedNotFinalized(AuctionStatus.ACTIVE, now)
                    .stream().map(AuctionItem::getId).toList();
        } catch (Exception e) {
            log.error("Failed to query ended auctions", e);
            return;
        }
        for (Long id : endedIds) {
            try {
                self.finalizeOne(id);  // each one runs in its own transaction
            } catch (Exception e) {
                log.error("Failed to finalize auction id={}", id, e);
            }
        }
    }

    @Override
    @Transactional
    public void finalizeOne(Long auctionId) {
        AuctionItem auction = auctionRepository.lockById(auctionId).orElse(null);
        if (auction == null || auction.getStatus() != AuctionStatus.ACTIVE) return;
        if (LocalDateTime.now().isBefore(auction.getAuctionEndTime())) return;

        // Determine winner: the only remaining ACTIVE bid (others are already OUTBID)
        Bid winningBid = null;
        if (auction.getCurrentHighestBidId() != null) {
            winningBid = bidRepository.findById(auction.getCurrentHighestBidId()).orElse(null);
        }

        if (winningBid != null && winningBid.getStatus() == BidStatus.ACTIVE) {
            // Capture the winner's reservation
            if (winningBid.getWalletReservationId() == null) {
                log.error("winning bid has no reservation id, auction={}", auctionId);
                return;
            }
            CaptureRequest cap = CaptureRequest.builder()
                    .reservationId(winningBid.getWalletReservationId())
                    .type("AUCTION_WIN_CAPTURE")
                    .idempotencyKey("auction-capture-" + auctionId + "-" + winningBid.getId())
                    .description("Auction win - auction #" + auctionId)
                    .build();
            try {
                WalletApiResponse<WalletTransactionDto> resp = walletClient.capture(cap);
                if (resp == null || !resp.isSuccess()) {
                    log.error("capture failed for winning bid={} resp={}", winningBid.getId(), resp);
                    return;
                }
                winningBid.setStatus(BidStatus.WON);
                bidRepository.save(winningBid);
                auction.setWinnerKeycloakId(winningBid.getBidderKeycloakId());
                auction.setWinnerBidId(winningBid.getId());
            } catch (RestClientException e) {
                log.error("wallet capture call failed for auction={}", auctionId, e);
                return;
            }
        }

        auction.setStatus(AuctionStatus.ENDED);
        auctionRepository.save(auction);

        broadcaster.publish(auctionId, AuctionEvent.builder()
                .type("auction.ended")
                .auctionId(auctionId)
                .winnerKeycloakId(auction.getWinnerKeycloakId())
                .currentHighestBid(auction.getCurrentHighestBid())
                .timestamp(LocalDateTime.now())
                .build());
        log.info("auction finalized id={} winner={}", auctionId, auction.getWinnerKeycloakId());
    }

    private void safeReleaseReservation(Long reservationId, String description) {
        try {
            ReleaseRequest rel = ReleaseRequest.builder()
                    .reservationId(reservationId)
                    .idempotencyKey("release-" + reservationId + "-" + UUID.randomUUID())
                    .description(description)
                    .build();
            WalletApiResponse<WalletTransactionDto> r = walletClient.release(rel);
            if (r == null || !r.isSuccess()) {
                log.error("Release failed for reservation={} resp={}", reservationId, r);
            }
        } catch (Exception e) {
            // Don't fail the bid because the previous release call had a transient error.
            // The finalizer will release abandoned ACTIVE reservations on auction end.
            log.error("Best-effort release failed reservation={}", reservationId, e);
        }
    }
}
