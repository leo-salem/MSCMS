package com.example.walletservice.dto.response;

import com.example.walletservice.model.enums.ReservationStatus;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class ReservationResponse {
    private Long id;
    private Long walletId;
    private BigDecimal amount;
    private String referenceId;
    private String referenceType;
    private ReservationStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
