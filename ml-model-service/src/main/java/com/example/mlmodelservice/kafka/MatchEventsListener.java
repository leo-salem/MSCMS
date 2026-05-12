package com.example.mlmodelservice.kafka;

import com.example.mlmodelservice.dto.event.MatchKafkaEvent;
import com.example.mlmodelservice.feature.MatchFeatureBuilder;
import com.example.mlmodelservice.model.entity.MlFootballMatchFeature;
import com.example.mlmodelservice.service.MlFootballMatchFeatureUpsertService;
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
public class MatchEventsListener {

    private static final Set<String> HANDLED = Set.of(
            "MATCH_CREATED",
            "MATCH_UPDATED",
            "MATCH_FINISHED",
            "MATCH_CANCELLED");

    private final MatchFeatureBuilder matchFeatureBuilder;
    private final MlFootballMatchFeatureUpsertService upsertService;

    @KafkaListener(
            topics = "${app.ml.kafka.match-events-topic}",
            groupId = "${spring.kafka.consumer.group-id:ml-model-service}",
            containerFactory = "kafkaListenerContainerFactory")
    public void consume(String payload) {
        try {
            MatchKafkaEvent event = matchFeatureBuilder.parse(payload);
            if (event.getEventType() == null) {
                log.warn("Ignoring match event without eventType: {}", payload);
                return;
            }
            String normalized = event.getEventType().trim().toUpperCase();
            if (!HANDLED.contains(normalized)) {
                log.debug("Skipping unsupported match event type: {}", normalized);
                return;
            }
            event.setEventType(normalized);
            MlFootballMatchFeature features = matchFeatureBuilder.fromKafkaEvent(event);
            upsertService.upsertFromKafka(features);
            log.debug("Upserted ML match features for key={}", features.getMatchExternalKey());
        } catch (IllegalArgumentException ex) {
            log.warn("Invalid match ML payload: {} — {}", ex.getMessage(), payload);
        } catch (JsonProcessingException ex) {
            log.warn("Malformed JSON on match-events; skipping. payload={}", payload, ex);
        } catch (Exception ex) {
            log.error("Failed to process match-events message", ex);
            throw ex;
        }
    }
}
