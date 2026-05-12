package com.example.storeservice.dto.request;

import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class AuctionRequest {
    @NotBlank @Size(max = 255) private String title;
    @Size(max = 2000) private String description;
    @Size(max = 1024) private String imageUrl;
    @NotNull @DecimalMin("0.00") private BigDecimal startingPrice;
    @NotNull @Future private LocalDateTime auctionStartTime;
    @NotNull @Future private LocalDateTime auctionEndTime;
    @DecimalMin("0.01") private BigDecimal minimumBidIncrement; // optional
}
