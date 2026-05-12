package com.example.storeservice.repository;

import com.example.storeservice.model.entity.Donation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;

@Repository
public interface DonationRepository extends JpaRepository<Donation, Long> {

    Page<Donation> findByUserKeycloakIdOrderByCreatedAtDesc(String userKeycloakId, Pageable pageable);

    @Query("select coalesce(sum(d.amount), 0) from Donation d")
    BigDecimal getTotalAmount();

    @Query("select count(d) from Donation d")
    Long getTotalCount();

    @Query("select count(distinct d.userKeycloakId) from Donation d")
    Long getUniqueDonorCount();
}
