package com.example.trainingmatchservice.repository;

import com.example.trainingmatchservice.model.match.entity.PlayerMatchStatistics;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PlayerMatchStatisticsRepository extends JpaRepository<PlayerMatchStatistics, Long> {
}

