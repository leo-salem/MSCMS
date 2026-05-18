package com.example.mlmodelservice.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "app.ml.seed")
public class MlSeedProperties {
    private boolean enabled;
    private String matchFeaturesResource = "classpath:data/ml_match_features_seed.json";
    private String playerFeaturesResource = "classpath:data/ml_player_features_seed.json";
}
