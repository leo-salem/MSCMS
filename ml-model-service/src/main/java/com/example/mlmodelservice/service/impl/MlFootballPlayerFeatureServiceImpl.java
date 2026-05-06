package com.example.mlmodelservice.service.impl;

import com.example.mlmodelservice.dto.MlFootballPlayerFeatureRequest;
import com.example.mlmodelservice.dto.MlFootballPlayerFeatureResponse;
import com.example.mlmodelservice.model.entity.MlFootballPlayerFeature;
import com.example.mlmodelservice.model.enums.FeatureSource;
import com.example.mlmodelservice.repository.MlFootballPlayerFeatureRepository;
import com.example.mlmodelservice.service.MlFootballPlayerFeatureService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class MlFootballPlayerFeatureServiceImpl implements MlFootballPlayerFeatureService {

    private final MlFootballPlayerFeatureRepository repository;

    @Override
    @Transactional
    public MlFootballPlayerFeatureResponse create(MlFootballPlayerFeatureRequest request) {
        if (request.getPlayerExternalKey() == null || request.getPlayerExternalKey().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "playerExternalKey is required");
        }
        if (repository.existsByPlayerExternalKey(request.getPlayerExternalKey())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "playerExternalKey already exists");
        }
        int fv = request.getFeatureVersion() != null ? request.getFeatureVersion() : 1;
        FeatureSource src = request.getSource() != null ? request.getSource() : FeatureSource.MANUAL;
        MlFootballPlayerFeature entity = MlFootballPlayerFeature.builder()
                .playerExternalKey(request.getPlayerExternalKey())
                .featureVersion(fv)
                .source(src)
                .eventTimestamp(request.getEventTimestamp())
                .ingestedAt(Instant.now())
                .fullName(request.getFullName())
                .position(request.getPosition())
                .nationality(request.getNationality())
                .age(request.getAge())
                .heightCm(request.getHeightCm())
                .weightKg(request.getWeightKg())
                .appearances(request.getAppearances())
                .goals(request.getGoals())
                .assists(request.getAssists())
                .marketValueMillionsEuro(request.getMarketValueMillionsEuro())
                .extraFeaturesJson(request.getExtraFeaturesJson())
                .build();
        return toResponse(repository.save(entity));
    }

    @Override
    @Transactional
    public MlFootballPlayerFeatureResponse update(Long id, MlFootballPlayerFeatureRequest request) {
        MlFootballPlayerFeature entity = repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Player feature not found"));
        if (request.getPlayerExternalKey() != null
                && !request.getPlayerExternalKey().equals(entity.getPlayerExternalKey())
                && repository.existsByPlayerExternalKey(request.getPlayerExternalKey())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "playerExternalKey already exists");
        }
        toEntity(entity, request);
        return toResponse(repository.save(entity));
    }

    @Override
    @Transactional(readOnly = true)
    public MlFootballPlayerFeatureResponse get(Long id) {
        return repository.findById(id)
                .map(this::toResponse)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Player feature not found"));
    }

    @Override
    @Transactional(readOnly = true)
    public List<MlFootballPlayerFeatureResponse> list() {
        return repository.findAll().stream().map(this::toResponse).toList();
    }

    @Override
    @Transactional
    public void delete(Long id) {
        if (!repository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Player feature not found");
        }
        repository.deleteById(id);
    }

    private MlFootballPlayerFeature toEntity(MlFootballPlayerFeature target, MlFootballPlayerFeatureRequest r) {
        if (r.getPlayerExternalKey() != null) {
            target.setPlayerExternalKey(r.getPlayerExternalKey());
        }
        if (r.getFeatureVersion() != null) {
            target.setFeatureVersion(r.getFeatureVersion());
        }
        if (r.getSource() != null) {
            target.setSource(r.getSource());
        }
        if (r.getEventTimestamp() != null) {
            target.setEventTimestamp(r.getEventTimestamp());
        }
        if (r.getFullName() != null) {
            target.setFullName(r.getFullName());
        }
        if (r.getPosition() != null) {
            target.setPosition(r.getPosition());
        }
        if (r.getNationality() != null) {
            target.setNationality(r.getNationality());
        }
        if (r.getAge() != null) {
            target.setAge(r.getAge());
        }
        if (r.getHeightCm() != null) {
            target.setHeightCm(r.getHeightCm());
        }
        if (r.getWeightKg() != null) {
            target.setWeightKg(r.getWeightKg());
        }
        if (r.getAppearances() != null) {
            target.setAppearances(r.getAppearances());
        }
        if (r.getGoals() != null) {
            target.setGoals(r.getGoals());
        }
        if (r.getAssists() != null) {
            target.setAssists(r.getAssists());
        }
        if (r.getMarketValueMillionsEuro() != null) {
            target.setMarketValueMillionsEuro(r.getMarketValueMillionsEuro());
        }
        if (r.getExtraFeaturesJson() != null) {
            target.setExtraFeaturesJson(r.getExtraFeaturesJson());
        }
        return target;
    }

    private MlFootballPlayerFeatureResponse toResponse(MlFootballPlayerFeature e) {
        MlFootballPlayerFeatureResponse r = new MlFootballPlayerFeatureResponse();
        r.setId(e.getId());
        r.setPlayerExternalKey(e.getPlayerExternalKey());
        r.setFeatureVersion(e.getFeatureVersion());
        r.setSource(e.getSource());
        r.setEventTimestamp(e.getEventTimestamp());
        r.setIngestedAt(e.getIngestedAt());
        r.setFullName(e.getFullName());
        r.setPosition(e.getPosition());
        r.setNationality(e.getNationality());
        r.setAge(e.getAge());
        r.setHeightCm(e.getHeightCm());
        r.setWeightKg(e.getWeightKg());
        r.setAppearances(e.getAppearances());
        r.setGoals(e.getGoals());
        r.setAssists(e.getAssists());
        r.setMarketValueMillionsEuro(e.getMarketValueMillionsEuro());
        r.setExtraFeaturesJson(e.getExtraFeaturesJson());
        return r;
    }
}
