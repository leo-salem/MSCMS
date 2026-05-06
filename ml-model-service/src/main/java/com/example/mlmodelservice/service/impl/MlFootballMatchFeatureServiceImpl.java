package com.example.mlmodelservice.service.impl;

import com.example.mlmodelservice.dto.MlFootballMatchFeatureRequest;
import com.example.mlmodelservice.dto.MlFootballMatchFeatureResponse;
import com.example.mlmodelservice.model.entity.MlFootballMatchFeature;
import com.example.mlmodelservice.model.enums.FeatureSource;
import com.example.mlmodelservice.repository.MlFootballMatchFeatureRepository;
import com.example.mlmodelservice.service.MlFootballMatchFeatureService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class MlFootballMatchFeatureServiceImpl implements MlFootballMatchFeatureService {

    private final MlFootballMatchFeatureRepository repository;

    @Override
    @Transactional
    public MlFootballMatchFeatureResponse create(MlFootballMatchFeatureRequest request) {
        if (request.getMatchExternalKey() == null || request.getMatchExternalKey().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "matchExternalKey is required");
        }
        if (repository.existsByMatchExternalKey(request.getMatchExternalKey())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "matchExternalKey already exists");
        }
        int fv = request.getFeatureVersion() != null ? request.getFeatureVersion() : 1;
        FeatureSource src = request.getSource() != null ? request.getSource() : FeatureSource.MANUAL;
        MlFootballMatchFeature entity = MlFootballMatchFeature.builder()
                .matchExternalKey(request.getMatchExternalKey())
                .featureVersion(fv)
                .source(src)
                .eventTimestamp(request.getEventTimestamp())
                .ingestedAt(Instant.now())
                .season(request.getSeason())
                .matchDate(request.getMatchDate())
                .homeTeamName(request.getHomeTeamName())
                .awayTeamName(request.getAwayTeamName())
                .homeGoals(request.getHomeGoals())
                .awayGoals(request.getAwayGoals())
                .competition(request.getCompetition())
                .stadium(request.getStadium())
                .xgHome(request.getXgHome())
                .xgAway(request.getXgAway())
                .extraFeaturesJson(request.getExtraFeaturesJson())
                .build();
        return toResponse(repository.save(entity));
    }

    @Override
    @Transactional
    public MlFootballMatchFeatureResponse update(Long id, MlFootballMatchFeatureRequest request) {
        MlFootballMatchFeature entity = repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Match feature not found"));
        if (request.getMatchExternalKey() != null
                && !request.getMatchExternalKey().equals(entity.getMatchExternalKey())
                && repository.existsByMatchExternalKey(request.getMatchExternalKey())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "matchExternalKey already exists");
        }
        toEntity(entity, request);
        return toResponse(repository.save(entity));
    }

    @Override
    @Transactional(readOnly = true)
    public MlFootballMatchFeatureResponse get(Long id) {
        return repository.findById(id)
                .map(this::toResponse)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Match feature not found"));
    }

    @Override
    @Transactional(readOnly = true)
    public List<MlFootballMatchFeatureResponse> list() {
        return repository.findAll().stream().map(this::toResponse).toList();
    }

    @Override
    @Transactional
    public void delete(Long id) {
        if (!repository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Match feature not found");
        }
        repository.deleteById(id);
    }

    private MlFootballMatchFeature toEntity(MlFootballMatchFeature target, MlFootballMatchFeatureRequest r) {
        if (r.getMatchExternalKey() != null) {
            target.setMatchExternalKey(r.getMatchExternalKey());
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
        if (r.getSeason() != null) {
            target.setSeason(r.getSeason());
        }
        if (r.getMatchDate() != null) {
            target.setMatchDate(r.getMatchDate());
        }
        if (r.getHomeTeamName() != null) {
            target.setHomeTeamName(r.getHomeTeamName());
        }
        if (r.getAwayTeamName() != null) {
            target.setAwayTeamName(r.getAwayTeamName());
        }
        if (r.getHomeGoals() != null) {
            target.setHomeGoals(r.getHomeGoals());
        }
        if (r.getAwayGoals() != null) {
            target.setAwayGoals(r.getAwayGoals());
        }
        if (r.getCompetition() != null) {
            target.setCompetition(r.getCompetition());
        }
        if (r.getStadium() != null) {
            target.setStadium(r.getStadium());
        }
        if (r.getXgHome() != null) {
            target.setXgHome(r.getXgHome());
        }
        if (r.getXgAway() != null) {
            target.setXgAway(r.getXgAway());
        }
        if (r.getExtraFeaturesJson() != null) {
            target.setExtraFeaturesJson(r.getExtraFeaturesJson());
        }
        return target;
    }

    private MlFootballMatchFeatureResponse toResponse(MlFootballMatchFeature e) {
        MlFootballMatchFeatureResponse r = new MlFootballMatchFeatureResponse();
        r.setId(e.getId());
        r.setMatchExternalKey(e.getMatchExternalKey());
        r.setFeatureVersion(e.getFeatureVersion());
        r.setSource(e.getSource());
        r.setEventTimestamp(e.getEventTimestamp());
        r.setIngestedAt(e.getIngestedAt());
        r.setSeason(e.getSeason());
        r.setMatchDate(e.getMatchDate());
        r.setHomeTeamName(e.getHomeTeamName());
        r.setAwayTeamName(e.getAwayTeamName());
        r.setHomeGoals(e.getHomeGoals());
        r.setAwayGoals(e.getAwayGoals());
        r.setCompetition(e.getCompetition());
        r.setStadium(e.getStadium());
        r.setXgHome(e.getXgHome());
        r.setXgAway(e.getXgAway());
        r.setExtraFeaturesJson(e.getExtraFeaturesJson());
        return r;
    }
}
