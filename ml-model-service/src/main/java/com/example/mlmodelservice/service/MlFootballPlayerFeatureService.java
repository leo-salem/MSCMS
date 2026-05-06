package com.example.mlmodelservice.service;

import com.example.mlmodelservice.dto.MlFootballPlayerFeatureRequest;
import com.example.mlmodelservice.dto.MlFootballPlayerFeatureResponse;

import java.util.List;

public interface MlFootballPlayerFeatureService {

    MlFootballPlayerFeatureResponse create(MlFootballPlayerFeatureRequest request);

    MlFootballPlayerFeatureResponse update(Long id, MlFootballPlayerFeatureRequest request);

    MlFootballPlayerFeatureResponse get(Long id);

    List<MlFootballPlayerFeatureResponse> list();

    void delete(Long id);
}
