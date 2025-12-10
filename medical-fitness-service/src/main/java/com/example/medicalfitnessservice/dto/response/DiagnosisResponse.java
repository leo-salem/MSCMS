package com.example.medicalfitnessservice.dto.response;

import java.time.LocalDateTime;

public record DiagnosisResponse(
        Long id,
        Long injuryId,
        Long playerId,
        Long doctorId,
        String diagnosis,
        String medicalNotes,
        String recommendations,
        LocalDateTime diagnosedAt,
        String testResults,
        String attachments
) {}

