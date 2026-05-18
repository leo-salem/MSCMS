package com.example.mlmodelservice.feature;

import com.example.mlmodelservice.dto.event.PlayerKafkaEvent;
import com.example.mlmodelservice.model.entity.MlFootballPlayerFeature;
import com.example.mlmodelservice.model.enums.FeatureSource;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDate;
import java.time.Period;

/**
 * Maps {@link PlayerKafkaEvent} payloads to feature rows.
 */
@Component
@RequiredArgsConstructor
public class PlayerFeatureBuilder {

    private final ObjectMapper objectMapper;

    public MlFootballPlayerFeature fromKafkaEvent(PlayerKafkaEvent event) {
        if (event.getEventType() == null) {
            throw new IllegalArgumentException("eventType is required");
        }
        String playerKey = resolvePlayerKey(event);
        if (playerKey == null) {
            throw new IllegalArgumentException("playerId or keycloakId is required");
        }

        String fullName = joinNames(event.getFirstName(), event.getLastName());

        ObjectNode experimental = objectMapper.createObjectNode();
        if (event.getEmail() != null) {
            experimental.put("email", event.getEmail());
        }
        if (event.getKitNumber() != null) {
            experimental.put("kitNumber", event.getKitNumber());
        }
        if (event.getMarketValue() != null) {
            experimental.put("marketValueRaw", event.getMarketValue());
        }
        if (event.getOldStatus() != null) {
            experimental.put("oldStatus", event.getOldStatus());
        }
        if (event.getNewStatus() != null) {
            experimental.put("newStatus", event.getNewStatus());
        }
        String extra = experimental.isEmpty() ? null : experimental.toString();

        Double marketMi = null;
        if (event.getMarketValue() != null) {
            marketMi = event.getMarketValue() / 1_000_000.0;
        }

        Integer age = null;
        if (event.getDateOfBirth() != null) {
            age = Period.between(event.getDateOfBirth(), LocalDate.now()).getYears();
        }

        return MlFootballPlayerFeature.builder()
                .playerExternalKey(playerKey)
                .featureVersion(1)
                .source(FeatureSource.KAFKA)
                .eventTimestamp(event.getTimestamp() != null ? event.getTimestamp() : Instant.now())
                .ingestedAt(Instant.now())
                .fullName(fullName)
                .position(event.getPreferredPosition())
                .nationality(event.getNationality())
                .age(age)
                .heightCm(event.getHeightCm())
                .weightKg(event.getWeightKg())
                .appearances(event.getAppearances())
                .goals(event.getGoals())
                .assists(event.getAssists())
                .marketValueMillionsEuro(marketMi)
                .extraFeaturesJson(extra)
                .build();
    }

    private static String resolvePlayerKey(PlayerKafkaEvent event) {
        if (event.getKeycloakId() != null && !event.getKeycloakId().isBlank()) {
            return "player-kc-" + event.getKeycloakId().trim();
        }
        if (event.getPlayerId() != null && !event.getPlayerId().isBlank()) {
            return "player-" + event.getPlayerId().trim();
        }
        return null;
    }

    private static String joinNames(String first, String last) {
        if (first == null && last == null) {
            return null;
        }
        return ((first != null ? first : "") + " " + (last != null ? last : "")).trim();
    }

    public PlayerKafkaEvent parse(String json) throws JsonProcessingException {
        return objectMapper.readValue(json, PlayerKafkaEvent.class);
    }
}
