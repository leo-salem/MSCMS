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
public class MatchCancelledEvent {

    private Long matchId;
    private Long homeTeamId;
    private Long outerTeamId;
    private String reason;
    private Instant timestamp;
}
