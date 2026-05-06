package com.example.mlmodelservice.kafka;

import com.example.mlmodelservice.dto.event.PlayerKafkaEvent;
import com.example.mlmodelservice.feature.PlayerFeatureBuilder;
import com.example.mlmodelservice.model.entity.MlFootballPlayerFeature;
import com.example.mlmodelservice.service.MlFootballPlayerFeatureUpsertService;
import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.Set;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.ml.kafka.listeners-enabled", havingValue = "true", matchIfMissing = true)
public class PlayerEventsListener {

    private static final Set<String> HANDLED = Set.of(
            "PLAYER_CREATED",
            "PLAYER_UPDATED",
            "PLAYER_STATS_UPDATED");

    private final PlayerFeatureBuilder playerFeatureBuilder;
    private final MlFootballPlayerFeatureUpsertService upsertService;

    @KafkaListener(
            topics = "${app.ml.kafka.player-events-topic}",
            groupId = "${spring.kafka.consumer.group-id:ml-model-service}",
            containerFactory = "kafkaListenerContainerFactory")
    public void consume(String payload) {
        try {
            PlayerKafkaEvent event = playerFeatureBuilder.parse(payload);
            if (event.getEventType() == null) {
                log.warn("Ignoring player event without eventType: {}", payload);
                return;
            }
            String normalized = event.getEventType().trim().toUpperCase();
            if (!HANDLED.contains(normalized)) {
                log.debug("Skipping unsupported player event type: {}", normalized);
                return;
            }
            event.setEventType(normalized);
            MlFootballPlayerFeature features = playerFeatureBuilder.fromKafkaEvent(event);
            upsertService.upsertFromKafka(features);
            log.debug("Upserted ML player features for key={}", features.getPlayerExternalKey());
        } catch (IllegalArgumentException ex) {
            log.warn("Invalid player ML payload: {} — {}", ex.getMessage(), payload);
        } catch (JsonProcessingException ex) {
            log.warn("Malformed JSON on player-events; skipping. payload={}", payload, ex);
        } catch (Exception ex) {
            log.error("Failed to process player-events message", ex);
            throw ex;
        }
    }
}
