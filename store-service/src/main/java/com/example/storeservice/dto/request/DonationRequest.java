package com.example.storeservice.dto.request;

import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;

@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class DonationRequest {
    @NotNull
    @DecimalMin(value = "1.00", message = "amount must be at least 1.00")
    private BigDecimal amount;

    @Size(max = 1024)
    private String message;

    @Builder.Default
    private Boolean anonymous = false;
}
