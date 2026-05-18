package com.example.storeservice.repository;

import com.example.storeservice.model.entity.AuctionItem;
import com.example.storeservice.model.enums.AuctionStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface AuctionItemRepository extends JpaRepository<AuctionItem, Long> {

    Page<AuctionItem> findByStatus(AuctionStatus status, Pageable pageable);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from AuctionItem a where a.id = :id")
    Optional<AuctionItem> lockById(@Param("id") Long id);

    @Query("select a from AuctionItem a where a.status = :status and a.auctionEndTime <= :now")
    List<AuctionItem> findEndedNotFinalized(@Param("status") AuctionStatus status,
                                            @Param("now") LocalDateTime now);

    @Query("select a from AuctionItem a where a.status = :status and a.auctionStartTime <= :now")
    List<AuctionItem> findReadyToStart(@Param("status") AuctionStatus status,
                                       @Param("now") LocalDateTime now);
}
