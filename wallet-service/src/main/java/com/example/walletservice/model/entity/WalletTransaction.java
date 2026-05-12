package com.example.walletservice.model.entity;

import com.example.walletservice.model.enums.TransactionStatus;
import com.example.walletservice.model.enums.TransactionType;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "wallet_transactions",
        uniqueConstraints = @UniqueConstraint(name = "uq_wallet_txn_idem", columnNames = "idempotency_key"),
        indexes = {
                @Index(name = "idx_wallet_txn_wallet_id", columnList = "wallet_id"),
                @Index(name = "idx_wallet_txn_reference", columnList = "reference_type, reference_id"),
                @Index(name = "idx_wallet_txn_created_at", columnList = "created_at")
        })
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@EntityListeners(AuditingEntityListener.class)
public class WalletTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "wallet_id", nullable = false)
    private Long walletId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private TransactionType type;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal amount;

    @Column(name = "balance_after", nullable = false, precision = 19, scale = 4)
    private BigDecimal balanceAfter;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private TransactionStatus status;

    @Column(name = "external_payment_id", length = 128)
    private String externalPaymentId;

    @Column(name = "idempotency_key", nullable = false, length = 128)
    private String idempotencyKey;

    @Column(name = "reference_id", length = 128)
    private String referenceId;

    @Column(name = "reference_type", length = 32)
    private String referenceType;

    @Column(length = 512)
    private String description;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
