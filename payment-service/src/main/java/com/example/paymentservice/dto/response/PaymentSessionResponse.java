package com.example.paymentservice.dto.response;

import com.example.paymentservice.model.enums.PaymentProvider;
import com.example.paymentservice.model.enums.PaymentSessionStatus;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class PaymentSessionResponse {
    private Long id;
    private String userKeycloakId;
    private BigDecimal amount;
    private String currency;
    private PaymentProvider provider;
    private String providerSessionId;
    private String checkoutUrl;
    private PaymentSessionStatus status;
    private LocalDateTime expiresAt;
    private LocalDateTime createdAt;
    private LocalDateTime completedAt;
}
