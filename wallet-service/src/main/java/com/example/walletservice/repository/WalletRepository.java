package com.example.walletservice.repository;

import com.example.walletservice.model.entity.Wallet;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface WalletRepository extends JpaRepository<Wallet, Long> {

    Optional<Wallet> findByUserKeycloakId(String userKeycloakId);

    /**
     * Pessimistic write lock used inside transactional money operations.
     * Prevents two concurrent transactions from reading the same balance and over-spending.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select w from Wallet w where w.userKeycloakId = :keycloakId")
    Optional<Wallet> lockByUserKeycloakId(@Param("keycloakId") String keycloakId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select w from Wallet w where w.id = :id")
    Optional<Wallet> lockById(@Param("id") Long id);

    boolean existsByUserKeycloakId(String userKeycloakId);
}
