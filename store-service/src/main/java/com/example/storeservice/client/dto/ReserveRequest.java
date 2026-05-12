package com.example.storeservice.client.dto;

import lombok.*;

import java.math.BigDecimal;

@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class ReserveRequest {
    private String userKeycloakId;
    private BigDecimal amount;
    private String referenceId;
    private String referenceType;
    private String idempotencyKey;
    private String description;
}
