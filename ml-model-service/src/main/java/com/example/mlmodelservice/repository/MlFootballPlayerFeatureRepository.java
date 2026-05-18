package com.example.mlmodelservice.repository;

import com.example.mlmodelservice.model.entity.MlFootballPlayerFeature;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MlFootballPlayerFeatureRepository extends JpaRepository<MlFootballPlayerFeature, Long> {

    Optional<MlFootballPlayerFeature> findByPlayerExternalKey(String playerExternalKey);

    boolean existsByPlayerExternalKey(String playerExternalKey);
}
