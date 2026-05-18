package com.example.storeservice.controller;

import com.example.storeservice.dto.request.CreateOrderRequest;
import com.example.storeservice.dto.response.ApiResponse;
import com.example.storeservice.dto.response.OrderResponse;
import com.example.storeservice.exception.customException.UnauthorizedAccessException;
import com.example.storeservice.service.OrderService;
import com.example.storeservice.service.SecurityService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/orders")
@RequiredArgsConstructor
@Tag(name = "Orders", description = "Buy products with the wallet")
public class OrderController {

    private final OrderService orderService;
    private final SecurityService securityService;

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Create an order. Charges the wallet for the total amount.")
    public ResponseEntity<ApiResponse<OrderResponse>> create(@Valid @RequestBody CreateOrderRequest req) {
        return ResponseEntity.ok(ApiResponse.success(
                orderService.create(requireCaller(), req), "Order placed"));
    }

    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "List my orders")
    public ResponseEntity<ApiResponse<Page<OrderResponse>>> myOrders(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponse.success(orderService.listMyOrders(
                requireCaller(), PageRequest.of(page, Math.min(size, 100), Sort.by("createdAt").descending()))));
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get one of my orders")
    public ResponseEntity<ApiResponse<OrderResponse>> get(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(orderService.getMyOrder(requireCaller(), id)));
    }

    @GetMapping("/admin")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "List all orders (admin)")
    public ResponseEntity<ApiResponse<Page<OrderResponse>>> listAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponse.success(orderService.listAllOrders(
                PageRequest.of(page, Math.min(size, 100), Sort.by("createdAt").descending()))));
    }

    private String requireCaller() {
        String keycloakId = securityService.getCurrentKeycloakId();
        if (keycloakId == null) throw new UnauthorizedAccessException("No authenticated user");
        return keycloakId;
    }
}
