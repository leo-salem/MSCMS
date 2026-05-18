package com.example.mlmodelservice.dto.event;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.time.Instant;
import java.time.LocalDate;

/**
 * Unified envelope on topic {@code player-events}.
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class PlayerKafkaEvent {

    private String eventType;
    private String playerId;
    private String keycloakId;
    private String firstName;
    private String lastName;
    private String email;
    private String nationality;
    private LocalDate dateOfBirth;
    private String preferredPosition;
    private Integer kitNumber;
    private Long marketValue;
    private Integer appearances;
    private Integer goals;
    private Integer assists;
    private Integer heightCm;
    private Integer weightKg;
    private Instant timestamp;
    private String oldStatus;
    private String newStatus;
}
