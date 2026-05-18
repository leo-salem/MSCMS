package com.example.walletservice.repository;

import com.example.walletservice.model.entity.WalletReservation;
import com.example.walletservice.model.enums.ReservationStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WalletReservationRepository extends JpaRepository<WalletReservation, Long> {

    Optional<WalletReservation> findByWalletIdAndReferenceTypeAndReferenceIdAndStatus(
            Long walletId, String referenceType, String referenceId, ReservationStatus status);

    List<WalletReservation> findByReferenceTypeAndReferenceIdAndStatus(
            String referenceType, String referenceId, ReservationStatus status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from WalletReservation r where r.id = :id")
    Optional<WalletReservation> lockById(@Param("id") Long id);
}
