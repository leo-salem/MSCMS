package com.example.trainingmatchservice.dto.request;

import com.example.trainingmatchservice.dto.validation.Create;
import com.example.trainingmatchservice.dto.validation.Update;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

public record PlayerMatchStatisticsRequest(
        @NotNull(groups = Create.class)
        @Positive(groups = {Create.class, Update.class})
        Long matchId,

        @NotNull(groups = Create.class)
        @Positive(groups = {Create.class, Update.class})
        Long playerId,

        @PositiveOrZero(groups = {Create.class, Update.class})
        Integer minutesPlayed,

        @PositiveOrZero(groups = {Create.class, Update.class})
        Integer goals,

        @PositiveOrZero(groups = {Create.class, Update.class})
        Integer assists,

        @PositiveOrZero(groups = {Create.class, Update.class})
        Integer shots,

        @PositiveOrZero(groups = {Create.class, Update.class})
        Integer shotsOnTarget,

        @PositiveOrZero(groups = {Create.class, Update.class})
        Integer dribbles,

        @PositiveOrZero(groups = {Create.class, Update.class})
        Integer dribblesSuccessful,

        @PositiveOrZero(groups = {Create.class, Update.class})
        Integer passes,

        @PositiveOrZero(groups = {Create.class, Update.class})
        Integer passesCompleted,

        @PositiveOrZero(groups = {Create.class, Update.class})
        Integer keyPasses,
        
        @PositiveOrZero(groups = {Create.class, Update.class})
        Integer tackles,

        @PositiveOrZero(groups = {Create.class, Update.class})
        Integer tacklesWon,

        @PositiveOrZero(groups = {Create.class, Update.class})
        Integer interceptions,
        
        @PositiveOrZero(groups = {Create.class, Update.class})
        Integer clearances,

        @PositiveOrZero(groups = {Create.class, Update.class})
        Integer foulsCommitted,
        @PositiveOrZero(groups = {Create.class, Update.class})
        Integer foulsWon,
        @PositiveOrZero(groups = {Create.class, Update.class})
        Integer yellowCards,
        @PositiveOrZero(groups = {Create.class, Update.class})
        Integer redCards,

        @Min(value = 0, groups = {Create.class, Update.class})
        @Max(value = 10, groups = {Create.class, Update.class})
        Double performanceRating
) {}

