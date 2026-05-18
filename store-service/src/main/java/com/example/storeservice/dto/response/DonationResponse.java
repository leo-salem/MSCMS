package com.example.storeservice.dto.response;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class DonationResponse {
    private Long id;
    /** Hidden when anonymous=true and the viewer is not the donor or admin. */
    private String userKeycloakId;
    private BigDecimal amount;
    private String currency;
    private String message;
    private Boolean anonymous;
    private LocalDateTime createdAt;
}
