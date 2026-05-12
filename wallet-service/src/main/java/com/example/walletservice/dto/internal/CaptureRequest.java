package com.example.walletservice.dto.internal;

import com.example.walletservice.model.enums.TransactionType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class CaptureRequest {
    @NotNull private Long reservationId;
    @NotNull private TransactionType type;
    @NotBlank @Size(max = 128) private String idempotencyKey;
    private String description;
}
