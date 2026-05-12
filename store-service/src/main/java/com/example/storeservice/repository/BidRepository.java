package com.example.storeservice.repository;

import com.example.storeservice.model.entity.Bid;
import com.example.storeservice.model.enums.BidStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BidRepository extends JpaRepository<Bid, Long> {

    Page<Bid> findByAuctionIdOrderByAmountDesc(Long auctionId, Pageable pageable);

    Optional<Bid> findFirstByAuctionIdOrderByAmountDescIdDesc(Long auctionId);

    List<Bid> findByAuctionIdAndStatusInOrderByAmountDesc(Long auctionId, List<BidStatus> statuses);

    Optional<Bid> findFirstByAuctionIdAndBidderKeycloakIdAndStatusOrderByAmountDesc(
            Long auctionId, String bidderKeycloakId, BidStatus status);
}
