package com.example.mlmodelservice.dto.event;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.time.Instant;

/**
 * Unified envelope on topic {@code match-events}. {@code eventType} values:
 * MATCH_CREATED, MATCH_UPDATED, MATCH_FINISHED, MATCH_CANCELLED.
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class MatchKafkaEvent {

    private String eventType;
    /** Accepts number or string in JSON. */
    private String matchId;
    private Long homeTeamId;
    /** Away / outer opponent team id (alias supported). */
    private Long awayTeamId;
    private Long outerTeamId;
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
