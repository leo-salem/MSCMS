package com.example.mlmodelservice.model.entity;

import com.example.mlmodelservice.model.enums.FeatureSource;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.time.LocalDate;

/**
 * Denormalized match-level features for ML training and inference (independent of core {@code Match} schema).
 * <p>Schema evolution: add nullable columns or bump {@link #featureVersion} when changing meaning of existing fields.</p>
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "ml_football_match_features", indexes = {
        @Index(name = "idx_ml_match_date", columnList = "matchDate"),
        @Index(name = "idx_ml_match_season", columnList = "season")
})
public class MlFootballMatchFeature {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 128)
    private String matchExternalKey;

    @Column(nullable = false)
    @Builder.Default
    private int featureVersion = 1;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    @Builder.Default
    private FeatureSource source = FeatureSource.KAFKA;

    /** Event time from upstream (Kafka payload); distinct from {@link #ingestedAt}. */
    private Instant eventTimestamp;

    @Column(nullable = false)
    private Instant ingestedAt;

    @Column(length = 32)
    private String season;

    private LocalDate matchDate;

    @Column(length = 128)
    private String homeTeamName;

    @Column(length = 128)
    private String awayTeamName;

    private Integer homeGoals;
    private Integer awayGoals;

    @Column(length = 128)
    private String competition;

    @Column(length = 256)
    private String stadium;

    private Double xgHome;
    private Double xgAway;

    @Column(columnDefinition = "text")
    private String extraFeaturesJson;

    @PrePersist
    public void prePersist() {
        if (ingestedAt == null) {
            ingestedAt = Instant.now();
        }
    }
}
