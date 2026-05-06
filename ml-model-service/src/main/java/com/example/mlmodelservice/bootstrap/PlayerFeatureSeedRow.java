package com.example.mlmodelservice.bootstrap;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.time.Instant;
import java.util.Map;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class PlayerFeatureSeedRow {

    private String playerExternalKey;
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
    private Map<String, Object> extraFeatures;
    private Instant eventTimestamp;
}
