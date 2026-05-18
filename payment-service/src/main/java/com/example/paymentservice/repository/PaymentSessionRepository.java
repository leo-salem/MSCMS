package com.example.paymentservice.repository;

import com.example.paymentservice.model.entity.PaymentSession;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PaymentSessionRepository extends JpaRepository<PaymentSession, Long> {
    Optional<PaymentSession> findByIdempotencyKey(String idempotencyKey);
    Optional<PaymentSession> findByProviderSessionId(String providerSessionId);
    Page<PaymentSession> findByUserKeycloakIdOrderByCreatedAtDesc(String userKeycloakId, Pageable pageable);
}
