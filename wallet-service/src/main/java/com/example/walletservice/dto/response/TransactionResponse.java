package com.example.walletservice.dto.response;

import com.example.walletservice.model.enums.TransactionStatus;
import com.example.walletservice.model.enums.TransactionType;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class TransactionResponse {
    private Long id;
    private Long walletId;
    private TransactionType type;
    private BigDecimal amount;
    private BigDecimal balanceAfter;
    private TransactionStatus status;
    private String externalPaymentId;
    private String idempotencyKey;
    private String referenceId;
    private String referenceType;
    private String description;
    private LocalDateTime createdAt;
}
