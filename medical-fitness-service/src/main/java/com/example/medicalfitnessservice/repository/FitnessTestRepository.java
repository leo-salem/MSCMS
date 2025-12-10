package com.example.medicalfitnessservice.repository;

import com.example.medicalfitnessservice.model.entity.FitnessTest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface FitnessTestRepository extends JpaRepository<FitnessTest, Long> {
}

