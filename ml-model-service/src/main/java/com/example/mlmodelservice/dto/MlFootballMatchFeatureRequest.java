package com.example.mlmodelservice.dto;

import com.example.mlmodelservice.model.enums.FeatureSource;
import lombok.Data;

import java.time.Instant;
import java.time.LocalDate;

@Data
public class MlFootballMatchFeatureRequest {
    private String matchExternalKey;
    private Integer featureVersion;
    private FeatureSource source;
    private Instant eventTimestamp;
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
    private String extraFeaturesJson;
}
