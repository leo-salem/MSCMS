package com.example.playerservice.model.entity;

import com.example.playerservice.model.enums.StatusOfPlayer;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "players_status")
public class PlayerStatus {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long playerId;

    @Enumerated(EnumType.STRING)
    private StatusOfPlayer statusOfPlayer;

}
