package com.example.storeservice.dto.response;

import lombok.*;

import java.math.BigDecimal;

@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class DonationStatsResponse {
    private BigDecimal totalAmount;
    private Long totalDonations;
    private Long uniqueDonors;
}
