package com.example.storeservice.dto.response;

import com.example.storeservice.model.enums.AuctionStatus;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class AuctionResponse {
    private Long id;
    private String title;
    private String description;
    private String imageUrl;
    private BigDecimal startingPrice;
    private BigDecimal currentHighestBid;
    private String currentHighestBidder;
    private LocalDateTime auctionStartTime;
    private LocalDateTime auctionEndTime;
    private String winnerKeycloakId;
    private AuctionStatus status;
    private BigDecimal minimumBidIncrement;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
