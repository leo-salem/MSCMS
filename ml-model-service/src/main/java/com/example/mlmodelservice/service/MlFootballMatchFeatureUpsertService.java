package com.example.mlmodelservice.service;

import com.example.mlmodelservice.model.entity.MlFootballMatchFeature;
import com.example.mlmodelservice.model.enums.FeatureSource;
import com.example.mlmodelservice.repository.MlFootballMatchFeatureRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class MlFootballMatchFeatureUpsertService {

    private final MlFootballMatchFeatureRepository repository;

    /**
     * Insert or update by {@link MlFootballMatchFeature#getMatchExternalKey()}.
     * Later events with the same key merge non-null scalars; featureVersion increments on each Kafka ingest.
     */
    @Transactional
    public MlFootballMatchFeature upsertFromKafka(MlFootballMatchFeature incoming) {
        incoming.setSource(FeatureSource.KAFKA);
        incoming.setIngestedAt(Instant.now());

        return repository.findByMatchExternalKey(incoming.getMatchExternalKey())
                .map(existing -> mergeAndSave(existing, incoming))
                .orElseGet(() -> repository.save(incoming));
    }

    private MlFootballMatchFeature mergeAndSave(MlFootballMatchFeature target, MlFootballMatchFeature in) {
        if (in.getEventTimestamp() != null) {
            if (target.getEventTimestamp() == null || in.getEventTimestamp().isAfter(target.getEventTimestamp())) {
                target.setEventTimestamp(in.getEventTimestamp());
            }
        }
        if (in.getSeason() != null) {
            target.setSeason(in.getSeason());
        }
        if (in.getMatchDate() != null) {
            target.setMatchDate(in.getMatchDate());
        }
        if (in.getHomeTeamName() != null) {
            target.setHomeTeamName(in.getHomeTeamName());
        }
        if (in.getAwayTeamName() != null) {
            target.setAwayTeamName(in.getAwayTeamName());
        }
        if (in.getHomeGoals() != null) {
            target.setHomeGoals(in.getHomeGoals());
        }
        if (in.getAwayGoals() != null) {
            target.setAwayGoals(in.getAwayGoals());
        }
        if (in.getCompetition() != null) {
            target.setCompetition(in.getCompetition());
        }
        if (in.getStadium() != null) {
            target.setStadium(in.getStadium());
        }
        if (in.getXgHome() != null) {
            target.setXgHome(in.getXgHome());
        }
        if (in.getXgAway() != null) {
            target.setXgAway(in.getXgAway());
        }
        if (in.getExtraFeaturesJson() != null) {
            target.setExtraFeaturesJson(in.getExtraFeaturesJson());
        }
        target.setFeatureVersion(target.getFeatureVersion() + 1);
        target.setIngestedAt(Instant.now());
        return repository.save(target);
    }
}
