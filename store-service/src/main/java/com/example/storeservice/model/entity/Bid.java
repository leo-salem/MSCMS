package com.example.storeservice.model.entity;

import com.example.storeservice.model.enums.BidStatus;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "bids")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@EntityListeners(AuditingEntityListener.class)
public class Bid {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "auction_id", nullable = false)
    private Long auctionId;

    @Column(name = "bidder_keycloak_id", nullable = false, length = 64)
    private String bidderKeycloakId;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private BidStatus status;

    @Column(name = "wallet_reservation_id")
    private Long walletReservationId;

    @CreatedDate
    @Column(name = "bid_time", nullable = false, updatable = false)
    private LocalDateTime bidTime;
}
