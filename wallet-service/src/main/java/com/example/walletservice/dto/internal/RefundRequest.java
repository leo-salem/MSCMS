package com.example.walletservice.dto.internal;

import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;

@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class RefundRequest {
    @NotBlank private String userKeycloakId;
    @NotNull @DecimalMin("0.01") private BigDecimal amount;
    @NotBlank @Size(max = 128) private String idempotencyKey;
    private String referenceId;
    private String referenceType;
    private String description;
}
