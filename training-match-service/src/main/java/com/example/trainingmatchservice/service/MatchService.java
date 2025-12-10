package com.example.trainingmatchservice.service;

import com.example.trainingmatchservice.dto.request.MatchRequest;
import com.example.trainingmatchservice.dto.response.MatchResponse;

import java.util.List;

public interface MatchService {
    MatchResponse createMatch(MatchRequest request);
    MatchResponse updateMatch(Long id, MatchRequest request);
    void deleteMatch(Long id);
    MatchResponse getMatchById(Long id);
    List<MatchResponse> getAllMatches();
}

