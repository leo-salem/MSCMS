package com.example.storeservice.client.dto;

import lombok.*;

import java.math.BigDecimal;

@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class DebitRequest {
    private String userKeycloakId;
    private BigDecimal amount;
    private String type;
    private String idempotencyKey;
    private String referenceId;
    private String referenceType;
    private String description;
}
