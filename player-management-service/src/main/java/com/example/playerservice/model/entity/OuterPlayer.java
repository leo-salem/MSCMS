package com.example.playerservice.model.entity;

import com.example.playerservice.model.embeddable.PlayerRef;
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
@Table(name = "outer_players")
public class OuterPlayer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Embedded
    private PlayerRef playerRef;

    @ManyToOne
    @JoinColumn(name = "outer_team_id")
    private OuterTeam outerTeam;
}
