package com.example.storeservice.dto.request;

import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;

@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class PlaceBidRequest {
    @NotNull @DecimalMin("0.01") private BigDecimal amount;
}
