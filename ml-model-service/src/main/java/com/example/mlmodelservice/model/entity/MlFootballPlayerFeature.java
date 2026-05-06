package com.example.mlmodelservice.model.entity;

import com.example.mlmodelservice.model.enums.FeatureSource;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

/**
 * Denormalized player-level features for ML. Schema evolution via {@link #featureVersion} and new columns.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "ml_football_player_features", indexes = {
        @Index(name = "idx_ml_player_position", columnList = "position")
})
public class MlFootballPlayerFeature {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 128)
    private String playerExternalKey;

    @Column(nullable = false)
    @Builder.Default
    private int featureVersion = 1;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    @Builder.Default
    private FeatureSource source = FeatureSource.KAFKA;

    private Instant eventTimestamp;

    @Column(nullable = false)
    private Instant ingestedAt;

    @Column(length = 256)
    private String fullName;

    @Column(length = 64)
    private String position;

    @Column(length = 96)
    private String nationality;

    private Integer age;
    private Integer heightCm;
    private Integer weightKg;

    private Integer appearances;
    private Integer goals;
    private Integer assists;

    private Double marketValueMillionsEuro;

    @Column(columnDefinition = "text")
    private String extraFeaturesJson;

    @PrePersist
    public void prePersist() {
        if (ingestedAt == null) {
            ingestedAt = Instant.now();
        }
    }
}
