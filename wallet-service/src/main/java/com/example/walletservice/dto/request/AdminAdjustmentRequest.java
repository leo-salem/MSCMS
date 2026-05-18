package com.example.walletservice.dto.request;

import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;

@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class AdminAdjustmentRequest {
    @NotNull private BigDecimal amount;
    @NotBlank @Size(max = 512) private String reason;
}
