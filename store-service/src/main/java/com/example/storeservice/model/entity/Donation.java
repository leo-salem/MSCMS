package com.example.storeservice.model.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "donations",
        uniqueConstraints = @UniqueConstraint(name = "uq_donations_idem", columnNames = "idempotency_key"))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@EntityListeners(AuditingEntityListener.class)
public class Donation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_keycloak_id", nullable = false, length = 64)
    private String userKeycloakId;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal amount;

    @Column(nullable = false, length = 8)
    @Builder.Default
    private String currency = "USD";

    @Column(length = 1024)
    private String message;

    @Column(nullable = false)
    @Builder.Default
    private Boolean anonymous = false;

    @Column(name = "wallet_transaction_id", length = 64)
    private String walletTransactionId;

    @Column(name = "idempotency_key", nullable = false, length = 128)
    private String idempotencyKey;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
