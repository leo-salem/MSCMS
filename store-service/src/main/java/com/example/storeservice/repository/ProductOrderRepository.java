package com.example.storeservice.repository;

import com.example.storeservice.model.entity.ProductOrder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ProductOrderRepository extends JpaRepository<ProductOrder, Long> {
    Optional<ProductOrder> findByIdempotencyKey(String idempotencyKey);
    Page<ProductOrder> findByUserKeycloakIdOrderByCreatedAtDesc(String userKeycloakId, Pageable pageable);
}
