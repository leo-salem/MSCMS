package com.example.usermanagementservice.service.kafka.outboxPublisher;

import com.example.usermanagementservice.dto.event.PlayerStatusChangedEvent;
import com.example.usermanagementservice.dto.event.UserCreatedEvent;
import com.example.usermanagementservice.dto.event.UserUpdatedEvent;
import com.example.usermanagementservice.dto.ml.PlayerStreamEvent;
import com.example.usermanagementservice.model.event.OutboxEvent;
import com.example.usermanagementservice.repository.OutboxEventRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class OutboxPublisherImpl implements OutboxPublisher {

    private static final String PLAYER_EVENTS_ML_TOPIC = "player-events";

    private final OutboxEventRepository outboxRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    @Override
    @Scheduled(fixedDelay = 5000)
    @Transactional
    public void publishPendingEvents() {
        try {
            List<OutboxEvent> pendingEvents = outboxRepository.findTop100BySentFalseOrderByCreatedAtAsc();
            if (!pendingEvents.isEmpty()) {
                log.debug("Publishing {} pending outbox events", pendingEvents.size());
                for (OutboxEvent event : pendingEvents) {
                    try {
                        String topic = getTopicForEventType(event.getEventType());
                        kafkaTemplate.send(topic, event.getAggregateId(), event.getPayload())
                                .whenComplete((result, ex) -> {
                                    if (ex == null) {
                                        try {
                                            publishPlayerEventsMlTopic(event);
                                        } catch (Exception e) {
                                            log.error("player-events ML publish failed for outbox id={}", event.getId(), e);
                                        }
                                        markEventAsSent(event);
                                        log.debug("Event published to Kafka: id={}, topic={}", event.getId(), topic);
                                    } else {
                                        handlePublishFailure(event, ex);
                                    }
                                });
                    } catch (Exception e) {
                        log.error("Failed to publish event: id={}", event.getId(), e);
                        event.setRetryCount(event.getRetryCount() + 1);
                        outboxRepository.save(event);
                    }
                }
            }
        } catch (Exception e) {
            log.error("Error in outbox publisher", e);
        }
    }

    private void publishPlayerEventsMlTopic(OutboxEvent event) throws com.fasterxml.jackson.core.JsonProcessingException {
        PlayerStreamEvent stream = buildPlayerStreamEvent(event);
        if (stream == null) {
            return;
        }
        String key = stream.getKeycloakId() != null ? stream.getKeycloakId() : stream.getPlayerId();
        kafkaTemplate.send(PLAYER_EVENTS_ML_TOPIC, key, objectMapper.writeValueAsString(stream));
        log.debug("Published to {} for player aggregate {}", PLAYER_EVENTS_ML_TOPIC, event.getAggregateId());
    }

    private PlayerStreamEvent buildPlayerStreamEvent(OutboxEvent event) throws com.fasterxml.jackson.core.JsonProcessingException {
        return switch (event.getEventType()) {
            case "user.created" -> {
                UserCreatedEvent p = objectMapper.readValue(event.getPayload(), UserCreatedEvent.class);
                if (!"PLAYER".equals(p.getRole())) {
                    yield null;
                }
                yield PlayerStreamEvent.builder()
                        .eventType("PLAYER_CREATED")
                        .playerId(String.valueOf(p.getUserId()))
                        .keycloakId(p.getKeycloakId())
                        .firstName(p.getFirstName())
                        .lastName(p.getLastName())
                        .email(p.getEmail())
                        .nationality(p.getNationality())
                        .dateOfBirth(p.getDateOfBirth())
                        .preferredPosition(p.getPreferredPosition())
                        .kitNumber(p.getKitNumber())
                        .marketValue(p.getMarketValue())
                        .timestamp(p.getTimestamp())
                        .build();
            }
            case "user.updated" -> {
                UserUpdatedEvent p = objectMapper.readValue(event.getPayload(), UserUpdatedEvent.class);
                if (!"PLAYER".equals(p.getRole())) {
                    yield null;
                }
                yield PlayerStreamEvent.builder()
                        .eventType("PLAYER_UPDATED")
                        .playerId(String.valueOf(p.getUserId()))
                        .keycloakId(p.getKeycloakId())
                        .firstName(p.getFirstName())
                        .lastName(p.getLastName())
                        .email(p.getEmail())
                        .nationality(p.getNationality())
                        .dateOfBirth(p.getDateOfBirth())
                        .preferredPosition(p.getPreferredPosition())
                        .kitNumber(p.getKitNumber())
                        .marketValue(p.getMarketValue())
                        .timestamp(p.getTimestamp())
                        .build();
            }
            case "player.status.changed" -> {
                PlayerStatusChangedEvent p = objectMapper.readValue(event.getPayload(), PlayerStatusChangedEvent.class);
                yield PlayerStreamEvent.builder()
                        .eventType("PLAYER_UPDATED")
                        .playerId(String.valueOf(p.getPlayerId()))
                        .keycloakId(p.getKeycloakId())
                        .firstName(p.getFirstName())
                        .lastName(p.getLastName())
                        .oldStatus(p.getOldStatus())
                        .newStatus(p.getNewStatus())
                        .timestamp(p.getTimestamp())
                        .build();
            }
            default -> null;
        };
    }

    @Transactional
    public void markEventAsSent(OutboxEvent event) {
        event.setSent(true);
        event.setSentAt(Instant.now());
        outboxRepository.save(event);
    }

    private void handlePublishFailure(OutboxEvent event, Throwable ex) {
        log.error("Failed to publish event to Kafka: id={}", event.getId(), ex);
        event.setRetryCount(event.getRetryCount() + 1);
        if (event.getRetryCount() > 10) {
            log.error("Event exceeded max retries: id={}", event.getId());
        }
        outboxRepository.save(event);
    }

    private String getTopicForEventType(String eventType) {
        return switch (eventType) {
            case "user.created" -> "user-created";
            case "user.updated" -> "user-updated";
            case "user.deleted" -> "user-deleted";
            case "player.status.changed" -> "player-status-changed";
            default -> "user-events";
        };
    }

    @Override
    @Scheduled(cron = "0 0 3 * * *")
    @Transactional
    public void cleanupOldEvents() {
        log.debug("Starting cleanup of old outbox events");
        try {
            Instant cutoffTime = Instant.now().minusSeconds(30L * 24 * 60 * 60);
            List<OutboxEvent> oldEvents = outboxRepository.findBySentTrueAndSentAtBefore(cutoffTime);
            if (!oldEvents.isEmpty()) {
                outboxRepository.deleteAll(oldEvents);
                log.info("Cleaned up {} old outbox events", oldEvents.size());
            }
        } catch (Exception e) {
            log.error("Error during outbox cleanup", e);
        }
    }
}
