package com.example.mlmodelservice.service;

import com.example.mlmodelservice.model.entity.MlFootballPlayerFeature;
import com.example.mlmodelservice.model.enums.FeatureSource;
import com.example.mlmodelservice.repository.MlFootballPlayerFeatureRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class MlFootballPlayerFeatureUpsertService {

    private final MlFootballPlayerFeatureRepository repository;

    @Transactional
    public MlFootballPlayerFeature upsertFromKafka(MlFootballPlayerFeature incoming) {
        incoming.setSource(FeatureSource.KAFKA);
        incoming.setIngestedAt(Instant.now());

        return repository.findByPlayerExternalKey(incoming.getPlayerExternalKey())
                .map(existing -> mergeAndSave(existing, incoming))
                .orElseGet(() -> repository.save(incoming));
    }

    private MlFootballPlayerFeature mergeAndSave(MlFootballPlayerFeature target, MlFootballPlayerFeature in) {
        if (in.getEventTimestamp() != null) {
            if (target.getEventTimestamp() == null || !in.getEventTimestamp().isBefore(target.getEventTimestamp())) {
                target.setEventTimestamp(in.getEventTimestamp());
            }
        }
        if (in.getFullName() != null) {
            target.setFullName(in.getFullName());
        }
        if (in.getPosition() != null) {
            target.setPosition(in.getPosition());
        }
        if (in.getNationality() != null) {
            target.setNationality(in.getNationality());
        }
        if (in.getAge() != null) {
            target.setAge(in.getAge());
        }
        if (in.getHeightCm() != null) {
            target.setHeightCm(in.getHeightCm());
        }
        if (in.getWeightKg() != null) {
            target.setWeightKg(in.getWeightKg());
        }
        if (in.getAppearances() != null) {
            target.setAppearances(in.getAppearances());
        }
        if (in.getGoals() != null) {
            target.setGoals(in.getGoals());
        }
        if (in.getAssists() != null) {
            target.setAssists(in.getAssists());
        }
        if (in.getMarketValueMillionsEuro() != null) {
            target.setMarketValueMillionsEuro(in.getMarketValueMillionsEuro());
        }
        if (in.getExtraFeaturesJson() != null) {
            target.setExtraFeaturesJson(in.getExtraFeaturesJson());
        }
        target.setFeatureVersion(target.getFeatureVersion() + 1);
        target.setIngestedAt(Instant.now());
        return repository.save(target);
    }
}
