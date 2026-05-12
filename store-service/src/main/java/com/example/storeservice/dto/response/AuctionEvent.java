package com.example.storeservice.dto.response;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Pushed over SSE on bid/auction state changes.
 * type: "bid.placed" | "auction.ended" | "heartbeat"
 */
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class AuctionEvent {
    private String type;
    private Long auctionId;
    private BigDecimal currentHighestBid;
    private String currentHighestBidder;
    private Long bidId;
    private String winnerKeycloakId;
    private LocalDateTime timestamp;
}
