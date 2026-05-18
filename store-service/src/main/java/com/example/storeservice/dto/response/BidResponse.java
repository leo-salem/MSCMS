package com.example.storeservice.dto.response;

import com.example.storeservice.model.enums.BidStatus;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class BidResponse {
    private Long id;
    private Long auctionId;
    private String bidderKeycloakId;
    private BigDecimal amount;
    private BidStatus status;
    private LocalDateTime bidTime;
}
