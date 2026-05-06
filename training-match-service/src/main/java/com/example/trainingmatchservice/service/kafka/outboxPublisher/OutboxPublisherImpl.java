package com.example.trainingmatchservice.service.kafka.outboxPublisher;

import com.example.trainingmatchservice.dto.event.MatchCancelledEvent;
import com.example.trainingmatchservice.dto.event.MatchCompletedEvent;
import com.example.trainingmatchservice.dto.event.MatchScheduledEvent;
import com.example.trainingmatchservice.dto.ml.MatchStreamEvent;
import com.example.trainingmatchservice.model.event.OutboxEvent;
import com.example.trainingmatchservice.repository.OutboxEventRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class OutboxPublisherImpl implements OutboxPublisher {

    private static final String MATCH_EVENTS_ML_TOPIC = "match-events";

    private final OutboxEventRepository outboxEventRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    @Override
    @Scheduled(fixedRate = 5000)
    @Transactional
    public void publishPendingEvents() {
        List<OutboxEvent> pendingEvents = outboxEventRepository.findTop100BySentFalseOrderByCreatedAtAsc();

        for (OutboxEvent event : pendingEvents) {
            try {
                String topic = resolveTopicName(event.getEventType());

                kafkaTemplate.send(topic, event.getAggregateId(), event.getPayload());

                publishMatchEventsMlTopic(event);

                event.setSent(true);
                event.setSentAt(Instant.now());
                outboxEventRepository.save(event);

                log.info("Successfully published outbox event [id={}, type={}, topic={}]",
                        event.getId(), event.getEventType(), topic);
            } catch (Exception e) {
                event.setRetryCount(event.getRetryCount() + 1);
                outboxEventRepository.save(event);

                log.error("Failed to publish outbox event [id={}, type={}, retryCount={}]: {}",
                        event.getId(), event.getEventType(), event.getRetryCount(), e.getMessage());
            }
        }
    }

    /**
     * Dedicated stream for ml-model-service (does not replace legacy notification topics).
     */
    private void publishMatchEventsMlTopic(OutboxEvent event) {
        try {
            MatchStreamEvent stream = buildMatchStreamEvent(event);
            if (stream == null) {
                return;
            }
            kafkaTemplate.send(MATCH_EVENTS_ML_TOPIC, event.getAggregateId(), objectMapper.writeValueAsString(stream));
            log.debug("Published to {} for aggregate {}", MATCH_EVENTS_ML_TOPIC, event.getAggregateId());
        } catch (Exception e) {
            log.error("Failed to publish to {} for outbox id={}: {}", MATCH_EVENTS_ML_TOPIC, event.getId(), e.getMessage(), e);
        }
    }

    private MatchStreamEvent buildMatchStreamEvent(OutboxEvent event) throws com.fasterxml.jackson.core.JsonProcessingException {
        return switch (event.getEventType()) {
            case "match.scheduled" -> {
                MatchScheduledEvent p = objectMapper.readValue(event.getPayload(), MatchScheduledEvent.class);
                yield MatchStreamEvent.builder()
                        .eventType("MATCH_CREATED")
                        .matchId(String.valueOf(p.getMatchId()))
                        .homeTeamId(p.getHomeTeamId())
                        .awayTeamId(p.getOuterTeamId())
                        .venue(p.getVenue())
                        .competition(p.getCompetition())
                        .season(p.getSeason())
                        .matchType(p.getMatchType())
                        .sportType(p.getSportType())
                        .status(p.getStatus())
                        .timestamp(p.getTimestamp())
                        .build();
            }
            case "match.completed" -> {
                MatchCompletedEvent p = objectMapper.readValue(event.getPayload(), MatchCompletedEvent.class);
                yield MatchStreamEvent.builder()
                        .eventType("MATCH_FINISHED")
                        .matchId(String.valueOf(p.getMatchId()))
                        .homeTeamId(p.getHomeTeamId())
                        .awayTeamId(p.getOuterTeamId())
                        .homeGoals(p.getHomeTeamScore())
                        .awayGoals(p.getAwayTeamScore())
                        .venue(null)
                        .competition(p.getCompetition())
                        .season(p.getSeason())
                        .matchType(p.getMatchType())
                        .possessionHome(p.getPossessionHome())
                        .shotsHome(p.getShotsHome())
                        .shotsAway(p.getShotsAway())
                        .timestamp(p.getTimestamp())
                        .build();
            }
            case "match.cancelled" -> {
                MatchCancelledEvent p = objectMapper.readValue(event.getPayload(), MatchCancelledEvent.class);
                yield MatchStreamEvent.builder()
                        .eventType("MATCH_CANCELLED")
                        .matchId(String.valueOf(p.getMatchId()))
                        .homeTeamId(p.getHomeTeamId())
                        .awayTeamId(p.getOuterTeamId())
                        .cancelReason(p.getReason())
                        .timestamp(p.getTimestamp())
                        .build();
            }
            default -> null;
        };
    }

    private String resolveTopicName(String eventType) {
        return switch (eventType) {
            case "match.scheduled" -> "match-scheduled";
            case "match.completed" -> "match-completed";
            case "match.cancelled" -> "match-cancelled";
            case "training.session.completed" -> "training-session-completed";
            case "training.session.cancelled" -> "training-session-cancelled";
            default -> "training-match-events";
        };
    }
}
