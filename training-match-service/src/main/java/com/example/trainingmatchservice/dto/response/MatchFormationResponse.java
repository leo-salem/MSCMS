package com.example.trainingmatchservice.dto.response;

public record MatchFormationResponse(
        Long id,
        Long teamId,
        Long setByCoachId,
        String formation,
        String tacticalApproach,
        String formationDetails
) {}

