package com.example.mlmodelservice.bootstrap;

import com.example.mlmodelservice.config.MlSeedProperties;
import com.example.mlmodelservice.model.entity.MlFootballMatchFeature;
import com.example.mlmodelservice.model.entity.MlFootballPlayerFeature;
import com.example.mlmodelservice.model.enums.FeatureSource;
import com.example.mlmodelservice.repository.MlFootballMatchFeatureRepository;
import com.example.mlmodelservice.repository.MlFootballPlayerFeatureRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.util.StreamUtils;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class MlDatasetSeedConfig {

    private final MlSeedProperties seedProperties;
    private final ResourceLoader resourceLoader;
    private final MlFootballMatchFeatureRepository matchFeatureRepository;
    private final MlFootballPlayerFeatureRepository playerFeatureRepository;

    @Bean
    CommandLineRunner mlDatasetSeedRunner() {
        return args -> {
            if (!seedProperties.isEnabled()) {
                log.info("ML seed skipped (app.ml.seed.enabled is false). Set APP_ML_SEED_ENABLED=true to load classpath JSON.");
                return;
            }

            try {
                ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule());
                long matchCount = matchFeatureRepository.count();
                long playerCount = playerFeatureRepository.count();

                int matchSaved = 0;
                int playerSaved = 0;

                if (matchCount == 0) {
                    matchSaved = loadMatches(mapper);
                    if (matchSaved > 0) {
                        log.info("ML seed: loaded {} match feature row(s) from {}", matchSaved, seedProperties.getMatchFeaturesResource());
                    } else {
                        log.warn("ML seed: match table empty but no rows were inserted (missing resource or no valid rows).");
                    }
                } else {
                    log.info("ML seed skipped: ml_football_match_features not empty ({} row(s)).", matchCount);
                }

                if (playerCount == 0) {
                    playerSaved = loadPlayers(mapper);
                    if (playerSaved > 0) {
                        log.info("ML seed: loaded {} player feature row(s) from {}", playerSaved, seedProperties.getPlayerFeaturesResource());
                    } else {
                        log.warn("ML seed: player table empty but no rows were inserted (missing resource or no valid rows).");
                    }
                } else {
                    log.info("ML seed skipped: ml_football_player_features not empty ({} row(s)).", playerCount);
                }

                if (matchSaved > 0 || playerSaved > 0) {
                    log.info("ML seed data loaded successfully (matches={}, players={}).", matchSaved, playerSaved);
                }
            } catch (Exception e) {
                throw new IllegalStateException("ML dataset seed failed", e);
            }
        };
    }

    private int loadMatches(ObjectMapper mapper) throws Exception {
        Resource resource = resourceLoader.getResource(seedProperties.getMatchFeaturesResource());
        if (!resource.exists()) {
            log.warn("Match seed resource missing: {}", seedProperties.getMatchFeaturesResource());
            return 0;
        }
        String json = StreamUtils.copyToString(resource.getInputStream(), StandardCharsets.UTF_8);
        List<MatchFeatureSeedRow> rows = mapper.readValue(json, new TypeReference<>() { });
        Instant ingestedBatch = Instant.now();
        int saved = 0;
        for (MatchFeatureSeedRow row : rows) {
            if (row.getMatchExternalKey() == null || row.getMatchExternalKey().isBlank()) {
                continue;
            }
            if (matchFeatureRepository.existsByMatchExternalKey(row.getMatchExternalKey())) {
                continue;
            }
            String extraJson = resolveExtraJson(mapper, row.getExtraFeatures(), row.getExtraFeaturesJson());
            MlFootballMatchFeature entity = MlFootballMatchFeature.builder()
                    .matchExternalKey(row.getMatchExternalKey().trim())
                    .featureVersion(1)
                    .source(FeatureSource.SEED)
                    .eventTimestamp(row.getEventTimestamp())
                    .ingestedAt(ingestedBatch)
                    .season(row.getSeason())
                    .matchDate(row.getMatchDate())
                    .homeTeamName(row.getHomeTeamName())
                    .awayTeamName(row.getAwayTeamName())
                    .homeGoals(row.getHomeGoals())
                    .awayGoals(row.getAwayGoals())
                    .competition(row.getCompetition())
                    .stadium(row.getStadium())
                    .xgHome(row.getXgHome())
                    .xgAway(row.getXgAway())
                    .extraFeaturesJson(extraJson)
                    .build();
            matchFeatureRepository.save(entity);
            saved++;
        }
        return saved;
    }

    private int loadPlayers(ObjectMapper mapper) throws Exception {
        Resource resource = resourceLoader.getResource(seedProperties.getPlayerFeaturesResource());
        if (!resource.exists()) {
            log.warn("Player seed resource missing: {}", seedProperties.getPlayerFeaturesResource());
            return 0;
        }
        String json = StreamUtils.copyToString(resource.getInputStream(), StandardCharsets.UTF_8);
        List<PlayerFeatureSeedRow> rows = mapper.readValue(json, new TypeReference<>() { });
        Instant ingestedBatch = Instant.now();
        int saved = 0;
        for (PlayerFeatureSeedRow row : rows) {
            if (row.getPlayerExternalKey() == null || row.getPlayerExternalKey().isBlank()) {
                continue;
            }
            if (playerFeatureRepository.existsByPlayerExternalKey(row.getPlayerExternalKey())) {
                continue;
            }
            String extraJson = resolveExtraJson(mapper, row.getExtraFeatures(), row.getExtraFeaturesJson());
            MlFootballPlayerFeature entity = MlFootballPlayerFeature.builder()
                    .playerExternalKey(row.getPlayerExternalKey().trim())
                    .featureVersion(1)
                    .source(FeatureSource.SEED)
                    .eventTimestamp(row.getEventTimestamp())
                    .ingestedAt(ingestedBatch)
                    .fullName(row.getFullName())
                    .position(row.getPosition())
                    .nationality(row.getNationality())
                    .age(row.getAge())
                    .heightCm(row.getHeightCm())
                    .weightKg(row.getWeightKg())
                    .appearances(row.getAppearances())
                    .goals(row.getGoals())
                    .assists(row.getAssists())
                    .marketValueMillionsEuro(row.getMarketValueMillionsEuro())
                    .extraFeaturesJson(extraJson)
                    .build();
            playerFeatureRepository.save(entity);
            saved++;
        }
        return saved;
    }

    private static String resolveExtraJson(
            ObjectMapper mapper,
            java.util.Map<String, Object> extraFeatures,
            String legacyJson) throws JsonProcessingException {
        if (extraFeatures != null && !extraFeatures.isEmpty()) {
            return mapper.writeValueAsString(extraFeatures);
        }
        if (legacyJson != null && !legacyJson.isBlank()) {
            return legacyJson;
        }
        return null;
    }
}
