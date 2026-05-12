package com.example.storeservice.dto.response;

import com.example.storeservice.model.enums.OrderStatus;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class OrderResponse {
    private Long id;
    private String userKeycloakId;
    private BigDecimal totalAmount;
    private String currency;
    private OrderStatus status;
    private String shippingAddress;
    private String walletTransactionId;
    private List<OrderItemResponse> items;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
