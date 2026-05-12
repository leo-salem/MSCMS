package com.example.storeservice.client.dto;

import lombok.*;

import java.math.BigDecimal;

@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class WalletTransactionDto {
    private Long id;
    private Long walletId;
    private String type;
    private BigDecimal amount;
    private BigDecimal balanceAfter;
    private String status;
}
