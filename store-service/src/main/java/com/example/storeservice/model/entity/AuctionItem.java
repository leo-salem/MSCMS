package com.example.storeservice.model.entity;

import com.example.storeservice.model.enums.AuctionStatus;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "auction_items")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@EntityListeners(AuditingEntityListener.class)
public class AuctionItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "image_url", length = 1024)
    private String imageUrl;

    @Column(name = "starting_price", nullable = false, precision = 19, scale = 4)
    private BigDecimal startingPrice;

    @Column(name = "current_highest_bid", precision = 19, scale = 4)
    private BigDecimal currentHighestBid;

    @Column(name = "current_highest_bidder", length = 64)
    private String currentHighestBidder;

    @Column(name = "current_highest_bid_id")
    private Long currentHighestBidId;

    @Column(name = "auction_start_time", nullable = false)
    private LocalDateTime auctionStartTime;

    @Column(name = "auction_end_time", nullable = false)
    private LocalDateTime auctionEndTime;

    @Column(name = "winner_keycloak_id", length = 64)
    private String winnerKeycloakId;

    @Column(name = "winner_bid_id")
    private Long winnerBidId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private AuctionStatus status;

    @Column(name = "minimum_bid_increment", nullable = false, precision = 19, scale = 4)
    @Builder.Default
    private BigDecimal minimumBidIncrement = new BigDecimal("1.00");

    @Version
    private Long version;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
