package com.example.walletservice.dto.internal;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class ReleaseRequest {
    @NotNull private Long reservationId;
    @NotBlank @Size(max = 128) private String idempotencyKey;
    private String description;
}
