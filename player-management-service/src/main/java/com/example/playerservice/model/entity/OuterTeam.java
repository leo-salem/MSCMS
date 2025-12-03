package com.example.playerservice.model.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.HashSet;
import java.util.Set;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "outer_teams")
public class OuterTeam {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private String email;
    private String country;

    @OneToMany(mappedBy = "outerTeam", cascade = CascadeType.ALL,orphanRemoval = true)
    private Set<OuterPlayer>outerPlayers = new HashSet<>();
}
