package com.example.mlmodelservice.bootstrap;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Map;

/**
 * JSON shape for {@code ml_match_features_seed.json} (from Excel or hand-authored).
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class MatchFeatureSeedRow {

    private String matchExternalKey;
    private String season;
    private LocalDate matchDate;
    private String homeTeamName;
    private String awayTeamName;
    private Integer homeGoals;
    private Integer awayGoals;
    private String competition;
    private String stadium;
    private Double xgHome;
    private Double xgAway;
    /** Optional legacy: raw JSON string. Prefer {@link #extraFeatures} object in JSON. */
    private String extraFeaturesJson;
    /** Nested object serialized to {@code extraFeaturesJson} on ingest. */
    private Map<String, Object> extraFeatures;
    /** From sheet date/time when available (ISO-8601 in JSON). */
    private Instant eventTimestamp;
}
