package com.example.reportsanalyticsservice.bootstrap;

import com.example.reportsanalyticsservice.model.entity.MatchAnalysis;
import com.example.reportsanalyticsservice.model.entity.PlayerAnalytics;
import com.example.reportsanalyticsservice.model.entity.ScoutReport;
import com.example.reportsanalyticsservice.model.enums.SportType;
import com.example.reportsanalyticsservice.repository.MatchAnalysisRepository;
import com.example.reportsanalyticsservice.repository.PlayerAnalyticsRepository;
import com.example.reportsanalyticsservice.repository.ScoutReportRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.seed.enabled", havingValue = "true")
public class ReportsDataSeeder implements CommandLineRunner {

    // Cross-service references
    private static final long MATCH_FB_FINISHED = 1L;   // training-match seed: first match (FINISHED)
    private static final long TEAM_FB = 1L;
    private static final String HEAD_COACH_FB_KC = "00000000-0000-0000-0000-000000000020";
    private static final String SCOUT_KC          = "00000000-0000-0000-0000-000000000040";

    private static final String PLAYER_1_KC = "00000000-0000-0000-0000-000000000101";
    private static final String PLAYER_2_KC = "00000000-0000-0000-0000-000000000102";

    private final MatchAnalysisRepository matchAnalysisRepository;
    private final PlayerAnalyticsRepository playerAnalyticsRepository;
    private final ScoutReportRepository scoutReportRepository;

    @Override
    @Transactional
    public void run(String... args) {
        if (matchAnalysisRepository.count() > 0 || playerAnalyticsRepository.count() > 0) {
            log.info("[SEED] reports-analytics already has data — skipping.");
            return;
        }
        log.info("[SEED] Seeding reports-analytics-service...");

        MatchAnalysis ma = new MatchAnalysis();
        ma.setMatchId(MATCH_FB_FINISHED);
        ma.setTeamId(TEAM_FB);
        ma.setSportType(SportType.FOOTBALL);
        ma.setSportSpecificStats("{\"possession\":58,\"shotsOnTarget\":7,\"corners\":6,\"fouls\":11}");
        ma.setKeyMoments("12' Salah goal (assist Hegazy). 67' Salah penalty. 80' Pyramids consolation header.");
        ma.setTacticalAnalysis("High press worked in the first half; dropped into a 4-4-2 mid-block after 70'.");
        ma.setPlayerRatings("{\"player1\":8.7,\"player2\":7.9,\"player3\":7.4,\"player4\":7.1}");
        ma.setAnalyzedByUserKeycloakId(HEAD_COACH_FB_KC);
        ma.setAnalyzedAt(LocalDateTime.now().minusDays(4));
        ma.setNotes("Solid showing — repeat the high press shape next week.");
        matchAnalysisRepository.save(ma);

        savePlayerAnalytics(PLAYER_1_KC, 6, 5, 3, 8.4, "{\"xG\":4.2,\"keyPasses\":11}");
        savePlayerAnalytics(PLAYER_2_KC, 6, 1, 4, 7.6, "{\"passAccuracy\":89,\"tackles\":14}");

        ScoutReport sr = new ScoutReport();
        sr.setScoutKeycloakId(SCOUT_KC);
        sr.setOuterPlayerId(null);
        sr.setTechnicalRating(8);
        sr.setPhysicalRating(7);
        sr.setTacticalRating(7);
        sr.setMentalityRating(9);
        sr.setStrengths("Quick decision-making, excellent off-ball movement, calm under pressure.");
        sr.setWeaknesses("Aerial duels — needs more upper-body strength.");
        sr.setOverallAssessment("Strong prospect for the senior squad. Recommend a trial spell.");
        sr.setRecommendSigning(true);
        sr.setCreatedAt(LocalDateTime.now().minusDays(2));
        scoutReportRepository.save(sr);

        log.info("[SEED] reports-analytics seeded: matchAnalyses={}, playerAnalytics={}, scoutReports={}",
                matchAnalysisRepository.count(), playerAnalyticsRepository.count(),
                scoutReportRepository.count());
    }

    private void savePlayerAnalytics(String playerKc, int matches, int primary, int secondary,
                                     double avgRating, String sportSpecificStats) {
        PlayerAnalytics pa = new PlayerAnalytics();
        pa.setPlayerKeycloakId(playerKc);
        pa.setTeamId(TEAM_FB);
        pa.setSportType(SportType.FOOTBALL);
        pa.setPeriodStart(LocalDate.now().minusMonths(2));
        pa.setPeriodEnd(LocalDate.now());
        pa.setTotalMatches(matches);
        pa.setPrimaryScore(primary);
        pa.setSecondaryScore(secondary);
        pa.setAverageRating(avgRating);
        pa.setTotalTrainingSessions(18);
        pa.setAttendanceRate(95);
        pa.setCurrentInjuries(0);
        pa.setAverageFitnessScore(8.2);
        pa.setFitnessTestsCount(4);
        pa.setSportSpecificStats(sportSpecificStats);
        pa.setCalculatedAt(LocalDateTime.now());
        playerAnalyticsRepository.save(pa);
    }
}
