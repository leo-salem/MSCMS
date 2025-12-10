package com.example.trainingmatchservice.dto.response;

public record PlayerMatchStatisticsResponse(
        Long id,
        Long matchId,
        Long playerId,
        Integer minutesPlayed,
        Integer goals,
        Integer assists,
        Integer shots,
        Integer shotsOnTarget,
        Integer dribbles,
        Integer dribblesSuccessful,
        Integer passes,
        Integer passesCompleted,
        Integer keyPasses,
        Integer tackles,
        Integer tacklesWon,
        Integer interceptions,
        Integer clearances,
        Integer foulsCommitted,
        Integer foulsWon,
        Integer yellowCards,
        Integer redCards,
        Double performanceRating
) {}

