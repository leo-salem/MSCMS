package com.example.paymentservice.dto.internal;

import lombok.*;

import java.math.BigDecimal;

@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class CreditRequest {
    private String userKeycloakId;
    private BigDecimal amount;
    private String type;
    private String idempotencyKey;
    private String externalPaymentId;
    private String referenceId;
    private String referenceType;
    private String description;
}
