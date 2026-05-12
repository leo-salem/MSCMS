package com.example.usermanagementservice.dto.event;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.*;
import java.time.Instant;
import java.time.LocalDate;

@Data @NoArgsConstructor @AllArgsConstructor @Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class UserCreatedEvent {
    private Long userId;
    private String keycloakId;
    private String firstName;
    private String lastName;
    private String email;
    private String role;
    private Instant timestamp;
    /** Populated when role is PLAYER */
    private LocalDate dateOfBirth;
    private String nationality;
    private String preferredPosition;
    private Integer kitNumber;
    private Long marketValue;
}
