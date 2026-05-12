package com.example.trainingmatchservice.dto.ml;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Envelope published to {@code match-events} for ML feature consumers (see ml-model-service).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class MatchStreamEvent {

    private String eventType;
    private String matchId;
    private Long homeTeamId;
    private Long awayTeamId;
    private Integer homeGoals;
    private Integer awayGoals;
    private Double possessionHome;
    private Integer shotsHome;
    private Integer shotsAway;
    private String venue;
    private String competition;
    private String season;
    private String matchType;
    private String sportType;
    private String status;
    private String cancelReason;
    private Instant timestamp;
}
