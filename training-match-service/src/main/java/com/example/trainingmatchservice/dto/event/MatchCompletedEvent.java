package com.example.trainingmatchservice.dto.event;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class MatchCompletedEvent {

    private Long matchId;
    private Long homeTeamId;
    private Long outerTeamId;
    private Integer homeTeamScore;
    private Integer awayTeamScore;
    private String matchType;
    private String competition;
    private String season;
    private Double possessionHome;
    private Integer shotsHome;
    private Integer shotsAway;
    private Instant timestamp;
}
