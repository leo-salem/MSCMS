package com.example.mlmodelservice.service;

import com.example.mlmodelservice.dto.MlFootballMatchFeatureRequest;
import com.example.mlmodelservice.dto.MlFootballMatchFeatureResponse;

import java.util.List;

public interface MlFootballMatchFeatureService {

    MlFootballMatchFeatureResponse create(MlFootballMatchFeatureRequest request);

    MlFootballMatchFeatureResponse update(Long id, MlFootballMatchFeatureRequest request);

    MlFootballMatchFeatureResponse get(Long id);

    List<MlFootballMatchFeatureResponse> list();

    void delete(Long id);
}
