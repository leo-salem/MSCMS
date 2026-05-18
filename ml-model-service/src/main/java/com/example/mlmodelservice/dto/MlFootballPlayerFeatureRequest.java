package com.example.mlmodelservice.dto;

import com.example.mlmodelservice.model.enums.FeatureSource;
import lombok.Data;

import java.time.Instant;

@Data
public class MlFootballPlayerFeatureRequest {
    private String playerExternalKey;
    private Integer featureVersion;
    private FeatureSource source;
    private Instant eventTimestamp;
    private String fullName;
    private String position;
    private String nationality;
    private Integer age;
    private Integer heightCm;
    private Integer weightKg;
    private Integer appearances;
    private Integer goals;
    private Integer assists;
    private Double marketValueMillionsEuro;
    private String extraFeaturesJson;
}
