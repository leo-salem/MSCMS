package com.example.mlmodelservice.repository;

import com.example.mlmodelservice.model.entity.MlFootballMatchFeature;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MlFootballMatchFeatureRepository extends JpaRepository<MlFootballMatchFeature, Long> {

    Optional<MlFootballMatchFeature> findByMatchExternalKey(String matchExternalKey);

    boolean existsByMatchExternalKey(String matchExternalKey);
}
