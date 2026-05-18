package com.example.walletservice.dto.response;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class WalletResponse {
    private Long id;
    private String userKeycloakId;
    private BigDecimal availableBalance;
    private BigDecimal reservedBalance;
    private BigDecimal totalBalance;
    private String currency;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
