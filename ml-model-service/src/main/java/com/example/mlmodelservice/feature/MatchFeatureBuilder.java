package com.example.mlmodelservice.feature;

import com.example.mlmodelservice.dto.event.MatchKafkaEvent;
import com.example.mlmodelservice.model.entity.MlFootballMatchFeature;
import com.example.mlmodelservice.model.enums.FeatureSource;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;

/**
 * Maps {@link MatchKafkaEvent} payloads to feature rows and attaches derived / experimental metrics in JSON.
 */
@Component
@RequiredArgsConstructor
public class MatchFeatureBuilder {

    private final ObjectMapper objectMapper;

    public MlFootballMatchFeature fromKafkaEvent(MatchKafkaEvent event) {
        if (event.getEventType() == null || event.getMatchId() == null || event.getMatchId().isBlank()) {
            throw new IllegalArgumentException("eventType and matchId are required");
        }

        String key = toMatchExternalKey(event.getMatchId());
        String homeName = event.getHomeTeamId() != null ? "team-" + event.getHomeTeamId() : null;
        Long oppId = event.getAwayTeamId() != null ? event.getAwayTeamId() : event.getOuterTeamId();
        String awayName = oppId != null ? "team-" + oppId : null;

        Integer hg = event.getHomeGoals();
        Integer ag = event.getAwayGoals();
        Integer goalDiff = (hg != null && ag != null) ? hg - ag : null;

        ObjectNode experimental = objectMapper.createObjectNode();
        if (event.getPossessionHome() != null) {
            experimental.put("possessionHome", event.getPossessionHome());
        }
        if (event.getShotsHome() != null) {
            experimental.put("shotsHome", event.getShotsHome());
        }
        if (event.getShotsAway() != null) {
            experimental.put("shotsAway", event.getShotsAway());
        }
        if (goalDiff != null) {
            experimental.put("goalDifference", goalDiff);
        }
        if (event.getMatchType() != null) {
            experimental.put("matchType", event.getMatchType());
        }
        if (event.getSportType() != null) {
            experimental.put("sportType", event.getSportType());
        }
        if (event.getStatus() != null) {
            experimental.put("status", event.getStatus());
        }
        if (event.getCancelReason() != null) {
            experimental.put("cancelReason", event.getCancelReason());
        }
        if (event.getHomeTeamId() != null) {
            experimental.put("homeTeamId", event.getHomeTeamId());
        }
        if (oppId != null) {
            experimental.put("awayTeamId", oppId);
        }
        Double shotRatio = shotRatio(event.getShotsHome(), event.getShotsAway());
        if (shotRatio != null) {
            experimental.put("shotsHomeShare", shotRatio);
        }

        String extraJson = experimental.isEmpty() ? null : experimental.toString();

        LocalDate matchDay = null;
        if (event.getTimestamp() != null) {
            matchDay = LocalDate.ofInstant(event.getTimestamp(), ZoneOffset.UTC);
        }

        return MlFootballMatchFeature.builder()
                .matchExternalKey(key)
                .featureVersion(1)
                .source(FeatureSource.KAFKA)
                .eventTimestamp(event.getTimestamp() != null ? event.getTimestamp() : Instant.now())
                .ingestedAt(Instant.now())
                .season(event.getSeason())
                .matchDate(matchDay)
                .homeTeamName(homeName)
                .awayTeamName(awayName)
                .homeGoals(hg)
                .awayGoals(ag)
                .competition(event.getCompetition())
                .stadium(event.getVenue())
                .xgHome(null)
                .xgAway(null)
                .extraFeaturesJson(extraJson)
                .build();
    }

    public static String toMatchExternalKey(String matchId) {
        return "match-" + matchId.trim();
    }

    private static Double shotRatio(Integer home, Integer away) {
        if (home == null && away == null) {
            return null;
        }
        int h = home != null ? home : 0;
        int a = away != null ? away : 0;
        int sum = h + a;
        if (sum == 0) {
            return null;
        }
        return (double) h / sum;
    }

    public MatchKafkaEvent parse(String json) throws JsonProcessingException {
        return objectMapper.readValue(json, MatchKafkaEvent.class);
    }
}
