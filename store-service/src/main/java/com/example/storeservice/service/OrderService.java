package com.example.storeservice.service;

import com.example.storeservice.dto.request.CreateOrderRequest;
import com.example.storeservice.dto.response.OrderResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface OrderService {
    OrderResponse create(String userKeycloakId, CreateOrderRequest req);
    OrderResponse getMyOrder(String userKeycloakId, Long orderId);
    Page<OrderResponse> listMyOrders(String userKeycloakId, Pageable pageable);
    Page<OrderResponse> listAllOrders(Pageable pageable);
}
