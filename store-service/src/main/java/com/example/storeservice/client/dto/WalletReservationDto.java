package com.example.storeservice.client.dto;

import lombok.*;

import java.math.BigDecimal;

@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class WalletReservationDto {
    private Long id;
    private Long walletId;
    private BigDecimal amount;
    private String referenceId;
    private String referenceType;
    private String status;
}
