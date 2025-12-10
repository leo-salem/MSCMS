package com.example.trainingmatchservice.repository;

import com.example.trainingmatchservice.model.match.entity.Match;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MatchRepository extends JpaRepository<Match, Long> {
}

