package com.example.walletservice.dto.internal;

import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;

@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class ReserveRequest {
    @NotBlank private String userKeycloakId;
    @NotNull @DecimalMin("0.01") private BigDecimal amount;
    @NotBlank private String referenceId;
    @NotBlank private String referenceType;
    @NotBlank @Size(max = 128) private String idempotencyKey;
    private String description;
}
