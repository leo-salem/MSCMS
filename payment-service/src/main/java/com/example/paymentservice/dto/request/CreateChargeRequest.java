package com.example.paymentservice.dto.request;

import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;

@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class CreateChargeRequest {
    @NotNull
    @DecimalMin(value = "1.00", message = "amount must be at least 1.00")
    @DecimalMax(value = "10000.00", message = "amount must not exceed 10000.00")
    private BigDecimal amount;

    private String currency;     // optional, defaults to configured currency
    private String successUrl;   // optional override
    private String cancelUrl;    // optional override
}
