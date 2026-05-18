package com.example.walletservice.service;

import com.example.walletservice.dto.internal.*;
import com.example.walletservice.dto.response.ReservationResponse;
import com.example.walletservice.dto.response.TransactionResponse;
import com.example.walletservice.dto.response.WalletResponse;
import com.example.walletservice.exception.customException.*;
import com.example.walletservice.mapper.WalletMapper;
import com.example.walletservice.model.entity.Wallet;
import com.example.walletservice.model.entity.WalletReservation;
import com.example.walletservice.model.entity.WalletTransaction;
import com.example.walletservice.model.enums.ReservationStatus;
import com.example.walletservice.model.enums.TransactionStatus;
import com.example.walletservice.model.enums.TransactionType;
import com.example.walletservice.repository.WalletReservationRepository;
import com.example.walletservice.repository.WalletRepository;
import com.example.walletservice.repository.WalletTransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class WalletServiceImpl implements WalletService {

    private final WalletRepository walletRepository;
    private final WalletTransactionRepository txnRepository;
    private final WalletReservationRepository reservationRepository;
    private final WalletMapper mapper;

    @Value("${mscms.wallet.default-currency:USD}")
    private String defaultCurrency;

    @Value("${mscms.wallet.max-balance:1000000.00}")
    private BigDecimal maxBalance;

    // ---------- USER FACING ----------

    @Override
    @Transactional
    public WalletResponse getOrCreateMyWallet(String keycloakId) {
        Wallet wallet = walletRepository.findByUserKeycloakId(keycloakId)
                .orElseGet(() -> createWallet(keycloakId));
        return mapper.toResponse(wallet);
    }

    @Override
    @Transactional(readOnly = true)
    public WalletResponse getWalletByKeycloakId(String keycloakId) {
        Wallet wallet = walletRepository.findByUserKeycloakId(keycloakId)
                .orElseThrow(() -> new ResourceNotFoundException("Wallet", "userKeycloakId", keycloakId));
        return mapper.toResponse(wallet);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<TransactionResponse> listMyTransactions(String keycloakId, Pageable pageable) {
        Wallet wallet = walletRepository.findByUserKeycloakId(keycloakId)
                .orElseThrow(() -> new ResourceNotFoundException("Wallet", "userKeycloakId", keycloakId));
        return txnRepository.findByWalletIdOrderByCreatedAtDesc(wallet.getId(), pageable)
                .map(mapper::toResponse);
    }

    // ---------- INTERNAL OPS ----------

    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public TransactionResponse credit(CreditRequest req) {
        Optional<WalletTransaction> existing = txnRepository.findByIdempotencyKey(req.getIdempotencyKey());
        if (existing.isPresent()) {
            log.info("credit replay on idempotencyKey={}", req.getIdempotencyKey());
            return mapper.toResponse(existing.get());
        }

        Wallet wallet = lockOrCreate(req.getUserKeycloakId());
        BigDecimal newBalance = wallet.getAvailableBalance().add(req.getAmount());
        if (newBalance.compareTo(maxBalance) > 0) {
            throw new InvalidOperationException("Crediting would exceed maximum wallet balance");
        }
        wallet.setAvailableBalance(newBalance);
        walletRepository.save(wallet);

        WalletTransaction txn = persistTxn(wallet, req.getType(), req.getAmount(), newBalance,
                TransactionStatus.SUCCESS, req.getExternalPaymentId(), req.getIdempotencyKey(),
                req.getReferenceId(), req.getReferenceType(),
                req.getDescription() != null ? req.getDescription() : ("Credit: " + req.getType()));
        log.info("wallet credit: walletId={} amount={} newAvailable={}", wallet.getId(), req.getAmount(), newBalance);
        return mapper.toResponse(txn);
    }

    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public TransactionResponse debit(DebitRequest req) {
        Optional<WalletTransaction> existing = txnRepository.findByIdempotencyKey(req.getIdempotencyKey());
        if (existing.isPresent()) return mapper.toResponse(existing.get());

        Wallet wallet = lockExisting(req.getUserKeycloakId());
        if (wallet.getAvailableBalance().compareTo(req.getAmount()) < 0) {
            throw new InsufficientFundsException(wallet.getAvailableBalance(), req.getAmount());
        }
        BigDecimal newBalance = wallet.getAvailableBalance().subtract(req.getAmount());
        wallet.setAvailableBalance(newBalance);
        walletRepository.save(wallet);

        WalletTransaction txn = persistTxn(wallet, req.getType(), req.getAmount(), newBalance,
                TransactionStatus.SUCCESS, null, req.getIdempotencyKey(),
                req.getReferenceId(), req.getReferenceType(),
                req.getDescription() != null ? req.getDescription() : ("Debit: " + req.getType()));
        log.info("wallet debit: walletId={} amount={} newAvailable={}", wallet.getId(), req.getAmount(), newBalance);
        return mapper.toResponse(txn);
    }

    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public ReservationResponse reserve(ReserveRequest req) {
        // Idempotency: same key returns the same reservation
        Optional<WalletTransaction> existingTxn = txnRepository.findByIdempotencyKey(req.getIdempotencyKey());
        if (existingTxn.isPresent()) {
            // Find the reservation associated with this txn's reference (one ACTIVE per wallet+ref)
            return reservationRepository
                    .findByWalletIdAndReferenceTypeAndReferenceIdAndStatus(
                            existingTxn.get().getWalletId(),
                            req.getReferenceType(), req.getReferenceId(),
                            ReservationStatus.ACTIVE)
                    .map(mapper::toResponse)
                    .orElseGet(() -> {
                        // Reservation was already released/captured; return latest state
                        return reservationRepository.findByReferenceTypeAndReferenceIdAndStatus(
                                        req.getReferenceType(), req.getReferenceId(), ReservationStatus.RELEASED)
                                .stream().filter(r -> r.getWalletId().equals(existingTxn.get().getWalletId()))
                                .findFirst().map(mapper::toResponse).orElse(null);
                    });
        }

        Wallet wallet = lockExisting(req.getUserKeycloakId());
        if (wallet.getAvailableBalance().compareTo(req.getAmount()) < 0) {
            throw new InsufficientFundsException(wallet.getAvailableBalance(), req.getAmount());
        }

        // Move available -> reserved
        wallet.setAvailableBalance(wallet.getAvailableBalance().subtract(req.getAmount()));
        wallet.setReservedBalance(wallet.getReservedBalance().add(req.getAmount()));
        walletRepository.save(wallet);

        WalletReservation reservation;
        try {
            reservation = WalletReservation.builder()
                    .walletId(wallet.getId())
                    .amount(req.getAmount())
                    .referenceId(req.getReferenceId())
                    .referenceType(req.getReferenceType())
                    .status(ReservationStatus.ACTIVE)
                    .build();
            reservation = reservationRepository.saveAndFlush(reservation);
        } catch (DataIntegrityViolationException e) {
            throw new InvalidOperationException(
                    "Active reservation already exists for this reference. Release it before creating a new one.");
        }

        persistTxn(wallet, TransactionType.AUCTION_BID_RESERVE, req.getAmount(),
                wallet.getAvailableBalance(), TransactionStatus.SUCCESS,
                null, req.getIdempotencyKey(), req.getReferenceId(), req.getReferenceType(),
                req.getDescription() != null ? req.getDescription() : ("Reserve for " + req.getReferenceType() + ":" + req.getReferenceId()));

        log.info("wallet reserve: walletId={} amount={} reservationId={}", wallet.getId(), req.getAmount(), reservation.getId());
        return mapper.toResponse(reservation);
    }

    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public TransactionResponse release(ReleaseRequest req) {
        Optional<WalletTransaction> existing = txnRepository.findByIdempotencyKey(req.getIdempotencyKey());
        if (existing.isPresent()) return mapper.toResponse(existing.get());

        WalletReservation reservation = reservationRepository.lockById(req.getReservationId())
                .orElseThrow(() -> new ResourceNotFoundException("Reservation", "id", req.getReservationId()));

        if (reservation.getStatus() != ReservationStatus.ACTIVE) {
            throw new InvalidOperationException("Reservation is not ACTIVE: id=" + reservation.getId() +
                    " status=" + reservation.getStatus());
        }

        Wallet wallet = walletRepository.lockById(reservation.getWalletId())
                .orElseThrow(() -> new ResourceNotFoundException("Wallet", "id", reservation.getWalletId()));

        // Move reserved -> available
        wallet.setReservedBalance(wallet.getReservedBalance().subtract(reservation.getAmount()));
        wallet.setAvailableBalance(wallet.getAvailableBalance().add(reservation.getAmount()));
        walletRepository.save(wallet);

        reservation.setStatus(ReservationStatus.RELEASED);
        reservationRepository.save(reservation);

        WalletTransaction txn = persistTxn(wallet, TransactionType.AUCTION_BID_RELEASE,
                reservation.getAmount(), wallet.getAvailableBalance(),
                TransactionStatus.SUCCESS, null, req.getIdempotencyKey(),
                reservation.getReferenceId(), reservation.getReferenceType(),
                req.getDescription() != null ? req.getDescription() : "Release reservation " + reservation.getId());
        log.info("wallet release: walletId={} amount={} reservationId={}", wallet.getId(), reservation.getAmount(), reservation.getId());
        return mapper.toResponse(txn);
    }

    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public TransactionResponse capture(CaptureRequest req) {
        Optional<WalletTransaction> existing = txnRepository.findByIdempotencyKey(req.getIdempotencyKey());
        if (existing.isPresent()) return mapper.toResponse(existing.get());

        WalletReservation reservation = reservationRepository.lockById(req.getReservationId())
                .orElseThrow(() -> new ResourceNotFoundException("Reservation", "id", req.getReservationId()));

        if (reservation.getStatus() != ReservationStatus.ACTIVE) {
            throw new InvalidOperationException("Reservation is not ACTIVE: id=" + reservation.getId() +
                    " status=" + reservation.getStatus());
        }

        Wallet wallet = walletRepository.lockById(reservation.getWalletId())
                .orElseThrow(() -> new ResourceNotFoundException("Wallet", "id", reservation.getWalletId()));

        // Permanently consume: subtract from reserved_balance, do NOT return to available
        wallet.setReservedBalance(wallet.getReservedBalance().subtract(reservation.getAmount()));
        walletRepository.save(wallet);

        reservation.setStatus(ReservationStatus.CAPTURED);
        reservationRepository.save(reservation);

        WalletTransaction txn = persistTxn(wallet, req.getType(), reservation.getAmount(),
                wallet.getAvailableBalance(), TransactionStatus.SUCCESS,
                null, req.getIdempotencyKey(),
                reservation.getReferenceId(), reservation.getReferenceType(),
                req.getDescription() != null ? req.getDescription() : "Capture reservation " + reservation.getId());
        log.info("wallet capture: walletId={} amount={} reservationId={}", wallet.getId(), reservation.getAmount(), reservation.getId());
        return mapper.toResponse(txn);
    }

    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public TransactionResponse refund(RefundRequest req) {
        Optional<WalletTransaction> existing = txnRepository.findByIdempotencyKey(req.getIdempotencyKey());
        if (existing.isPresent()) return mapper.toResponse(existing.get());

        Wallet wallet = lockOrCreate(req.getUserKeycloakId());
        BigDecimal newBalance = wallet.getAvailableBalance().add(req.getAmount());
        wallet.setAvailableBalance(newBalance);
        walletRepository.save(wallet);

        WalletTransaction txn = persistTxn(wallet, TransactionType.REFUND, req.getAmount(), newBalance,
                TransactionStatus.SUCCESS, null, req.getIdempotencyKey(),
                req.getReferenceId(), req.getReferenceType(),
                req.getDescription() != null ? req.getDescription() : "Refund");
        log.info("wallet refund: walletId={} amount={}", wallet.getId(), req.getAmount());
        return mapper.toResponse(txn);
    }

    @Override
    @Transactional
    public TransactionResponse adminAdjust(String targetKeycloakId, BigDecimal amount, String reason, String idempotencyKey) {
        if (amount.signum() == 0) throw new InvalidOperationException("Adjustment amount must be non-zero");
        Optional<WalletTransaction> existing = txnRepository.findByIdempotencyKey(idempotencyKey);
        if (existing.isPresent()) return mapper.toResponse(existing.get());

        Wallet wallet = lockOrCreate(targetKeycloakId);
        BigDecimal newBalance = wallet.getAvailableBalance().add(amount);
        if (newBalance.signum() < 0) {
            throw new InsufficientFundsException(wallet.getAvailableBalance(), amount.abs());
        }
        wallet.setAvailableBalance(newBalance);
        walletRepository.save(wallet);

        WalletTransaction txn = persistTxn(wallet, TransactionType.ADMIN_ADJUSTMENT, amount.abs(), newBalance,
                TransactionStatus.SUCCESS, null, idempotencyKey, null, "ADMIN",
                "Admin adjustment: " + reason);
        return mapper.toResponse(txn);
    }

    // ---------- HELPERS ----------

    @Transactional(propagation = Propagation.MANDATORY)
    protected Wallet lockOrCreate(String keycloakId) {
        return walletRepository.lockByUserKeycloakId(keycloakId)
                .orElseGet(() -> {
                    Wallet w = createWallet(keycloakId);
                    return walletRepository.lockById(w.getId())
                            .orElseThrow(() -> new WalletException("Wallet just created could not be locked: " + w.getId()));
                });
    }

    @Transactional(propagation = Propagation.MANDATORY)
    protected Wallet lockExisting(String keycloakId) {
        return walletRepository.lockByUserKeycloakId(keycloakId)
                .orElseThrow(() -> new ResourceNotFoundException("Wallet", "userKeycloakId", keycloakId));
    }

    private Wallet createWallet(String keycloakId) {
        Wallet wallet = Wallet.builder()
                .userKeycloakId(keycloakId)
                .availableBalance(BigDecimal.ZERO)
                .reservedBalance(BigDecimal.ZERO)
                .currency(defaultCurrency)
                .build();
        try {
            return walletRepository.saveAndFlush(wallet);
        } catch (DataIntegrityViolationException e) {
            // Concurrent create — fetch the existing one
            return walletRepository.findByUserKeycloakId(keycloakId)
                    .orElseThrow(() -> new WalletException("Wallet creation race lost and lookup failed"));
        }
    }

    private WalletTransaction persistTxn(Wallet wallet, TransactionType type, BigDecimal amount,
                                         BigDecimal balanceAfter, TransactionStatus status,
                                         String externalPaymentId, String idempotencyKey,
                                         String referenceId, String referenceType, String description) {
        WalletTransaction txn = WalletTransaction.builder()
                .walletId(wallet.getId())
                .type(type)
                .amount(amount)
                .balanceAfter(balanceAfter)
                .status(status)
                .externalPaymentId(externalPaymentId)
                .idempotencyKey(idempotencyKey)
                .referenceId(referenceId)
                .referenceType(referenceType)
                .description(description)
                .build();
        try {
            return txnRepository.saveAndFlush(txn);
        } catch (DataIntegrityViolationException e) {
            // Idempotency race: someone wrote the same key concurrently. Return their record.
            return txnRepository.findByIdempotencyKey(idempotencyKey)
                    .orElseThrow(() -> new WalletException("Idempotency race and lookup failed: " + idempotencyKey));
        }
    }
}
