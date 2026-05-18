package com.example.walletservice.dto.internal;

import com.example.walletservice.model.enums.TransactionType;
import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;

@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class CreditRequest {
    @NotBlank private String userKeycloakId;
    @NotNull @DecimalMin("0.01") private BigDecimal amount;
    @NotNull private TransactionType type;
    @NotBlank @Size(max = 128) private String idempotencyKey;
    private String externalPaymentId;
    private String referenceId;
    private String referenceType;
    private String description;
}
