package com.example.trainingmatchservice.dto.event;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class MatchScheduledEvent {

    private Long matchId;
    private Long homeTeamId;
    private Long outerTeamId;
    private String matchType;
    private String venue;
    private LocalDateTime kickoffTime;
    private String competition;
    private String season;
    private String sportType;
    private String status;
    private Instant timestamp;
}
