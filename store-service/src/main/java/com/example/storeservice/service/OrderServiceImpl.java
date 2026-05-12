package com.example.storeservice.service;

import com.example.storeservice.client.WalletInternalClient;
import com.example.storeservice.client.dto.DebitRequest;
import com.example.storeservice.client.dto.WalletApiResponse;
import com.example.storeservice.client.dto.WalletTransactionDto;
import com.example.storeservice.dto.request.CreateOrderRequest;
import com.example.storeservice.dto.response.OrderResponse;
import com.example.storeservice.exception.customException.*;
import com.example.storeservice.mapper.OrderMapper;
import com.example.storeservice.model.entity.OrderItem;
import com.example.storeservice.model.entity.Product;
import com.example.storeservice.model.entity.ProductOrder;
import com.example.storeservice.model.enums.OrderStatus;
import com.example.storeservice.model.enums.ProductStatus;
import com.example.storeservice.repository.ProductOrderRepository;
import com.example.storeservice.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClientException;

import java.math.BigDecimal;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderServiceImpl implements OrderService {

    private final ProductRepository productRepository;
    private final ProductOrderRepository orderRepository;
    private final WalletInternalClient walletClient;
    private final OrderMapper mapper;

    @Value("${mscms.store.currency:USD}")
    private String currency;

    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public OrderResponse create(String userKeycloakId, CreateOrderRequest req) {

        // Deterministic per-request idempotency key — caller can re-submit safely
        String idempotencyKey = "order-" + userKeycloakId + "-" + UUID.randomUUID();

        // 1) Lock all distinct products (in stable order to avoid deadlocks)
        Map<Long, Integer> qtyByProduct = new LinkedHashMap<>();
        for (CreateOrderRequest.OrderItemRequest it : req.getItems()) {
            qtyByProduct.merge(it.getProductId(), it.getQuantity(), Integer::sum);
        }
        List<Long> sortedIds = qtyByProduct.keySet().stream().sorted().toList();
        Map<Long, Product> locked = new LinkedHashMap<>();
        for (Long pid : sortedIds) {
            Product p = productRepository.lockById(pid)
                    .orElseThrow(() -> new ResourceNotFoundException("Product", "id", pid));
            if (p.getStatus() != ProductStatus.ACTIVE) {
                throw new InvalidOperationException("Product not available: id=" + pid + " status=" + p.getStatus());
            }
            int needed = qtyByProduct.get(pid);
            if (p.getStockQuantity() < needed) {
                throw new InsufficientStockException(
                        "Not enough stock for product '" + p.getName() + "' (have " + p.getStockQuantity() + ", need " + needed + ")");
            }
            locked.put(pid, p);
        }

        // 2) Build order + compute total
        ProductOrder order = ProductOrder.builder()
                .userKeycloakId(userKeycloakId)
                .currency(currency)
                .status(OrderStatus.PENDING)
                .shippingAddress(req.getShippingAddress())
                .idempotencyKey(idempotencyKey)
                .totalAmount(BigDecimal.ZERO)
                .build();

        BigDecimal total = BigDecimal.ZERO;
        for (CreateOrderRequest.OrderItemRequest it : req.getItems()) {
            Product p = locked.get(it.getProductId());
            OrderItem oi = OrderItem.builder()
                    .productId(p.getId())
                    .productName(p.getName())
                    .quantity(it.getQuantity())
                    .itemPrice(p.getPrice())
                    .build();
            order.addItem(oi);
            total = total.add(p.getPrice().multiply(BigDecimal.valueOf(it.getQuantity())));
        }
        order.setTotalAmount(total);
        order = orderRepository.save(order);

        // 3) Charge wallet via internal API
        DebitRequest debit = DebitRequest.builder()
                .userKeycloakId(userKeycloakId)
                .amount(total)
                .type("PURCHASE")
                .idempotencyKey("order-debit-" + order.getId())
                .referenceId(String.valueOf(order.getId()))
                .referenceType("ORDER")
                .description("Order #" + order.getId())
                .build();

        WalletApiResponse<WalletTransactionDto> debitResp;
        try {
            debitResp = walletClient.debit(debit);
        } catch (RestClientException e) {
            log.error("wallet debit call failed for order={}", order.getId(), e);
            throw new WalletOperationException("Wallet service unavailable: " + e.getMessage(), e);
        }
        if (debitResp == null || !debitResp.isSuccess() || debitResp.getData() == null) {
            String msg = debitResp != null ? debitResp.getMessage() : "null response from wallet";
            throw new WalletOperationException("Wallet debit failed: " + msg);
        }

        // 4) Decrement stock
        for (CreateOrderRequest.OrderItemRequest it : req.getItems()) {
            Product p = locked.get(it.getProductId());
            p.setStockQuantity(p.getStockQuantity() - it.getQuantity());
            if (p.getStockQuantity() == 0) p.setStatus(ProductStatus.OUT_OF_STOCK);
            productRepository.save(p);
        }

        order.setStatus(OrderStatus.PAID);
        order.setWalletTransactionId(String.valueOf(debitResp.getData().getId()));
        order = orderRepository.save(order);
        log.info("order created id={} user={} total={}", order.getId(), userKeycloakId, total);

        return mapper.toResponse(order);
    }

    @Override
    @Transactional(readOnly = true)
    public OrderResponse getMyOrder(String userKeycloakId, Long orderId) {
        ProductOrder order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", "id", orderId));
        if (!order.getUserKeycloakId().equals(userKeycloakId)) {
            throw new ResourceNotFoundException("Order", "id", orderId);
        }
        return mapper.toResponse(order);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<OrderResponse> listMyOrders(String userKeycloakId, Pageable pageable) {
        return orderRepository.findByUserKeycloakIdOrderByCreatedAtDesc(userKeycloakId, pageable)
                .map(mapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<OrderResponse> listAllOrders(Pageable pageable) {
        return orderRepository.findAll(pageable).map(mapper::toResponse);
    }
}
