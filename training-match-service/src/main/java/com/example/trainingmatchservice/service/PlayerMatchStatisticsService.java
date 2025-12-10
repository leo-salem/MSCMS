package com.example.trainingmatchservice.service;

import com.example.trainingmatchservice.dto.request.PlayerMatchStatisticsRequest;
import com.example.trainingmatchservice.dto.response.PlayerMatchStatisticsResponse;

import java.util.List;

public interface PlayerMatchStatisticsService {
    PlayerMatchStatisticsResponse createPlayerMatchStatistics(PlayerMatchStatisticsRequest request);
    PlayerMatchStatisticsResponse updatePlayerMatchStatistics(Long id, PlayerMatchStatisticsRequest request);
    void deletePlayerMatchStatistics(Long id);
    PlayerMatchStatisticsResponse getPlayerMatchStatisticsById(Long id);
    List<PlayerMatchStatisticsResponse> getAllPlayerMatchStatistics();
}

