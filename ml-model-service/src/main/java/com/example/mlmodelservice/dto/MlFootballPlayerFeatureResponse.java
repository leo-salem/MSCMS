package com.example.mlmodelservice.dto;

import com.example.mlmodelservice.model.enums.FeatureSource;
import lombok.Data;

import java.time.Instant;

@Data
public class MlFootballPlayerFeatureResponse {
    private Long id;
    private String playerExternalKey;
    private int featureVersion;
    private FeatureSource source;
    private Instant eventTimestamp;
    private Instant ingestedAt;
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
