package com.example.playerservice.model.entity;

import com.example.playerservice.model.enums.RequestStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "players-call-up-requests")
public class PlayerCallUpRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long playerId;

    private Long nationalTeamId;

    @Enumerated(EnumType.STRING)
    private RequestStatus status;

    private LocalDate requestDate;
}
