package com.example.medicalfitnessservice.dto.response;

import com.example.medicalfitnessservice.model.enums.FitnessTestType;

import java.time.LocalDateTime;

public record FitnessTestResponse(
        Long id,
        Long playerId,
        Long teamId,
        FitnessTestType testType,
        LocalDateTime testDate,
        Long conductedByDoctorId,
        String testName,
        Double result,
        String unit,
        String resultCategory,
        String notes,
        String recommendations,
        String attachments
) {}

