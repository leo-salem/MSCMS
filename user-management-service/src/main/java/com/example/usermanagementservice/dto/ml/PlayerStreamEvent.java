package com.example.usermanagementservice.dto.ml;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDate;

/**
 * Payload for topic {@code player-events} consumed by ml-model-service.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PlayerStreamEvent {

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
    private String oldStatus;
    private String newStatus;
    private Instant timestamp;
}
