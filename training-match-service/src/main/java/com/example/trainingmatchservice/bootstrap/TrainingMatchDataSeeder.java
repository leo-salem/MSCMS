package com.example.trainingmatchservice.bootstrap;

import com.example.trainingmatchservice.model.match.entity.Match;
import com.example.trainingmatchservice.model.match.enums.MatchStatus;
import com.example.trainingmatchservice.model.match.enums.MatchType;
import com.example.trainingmatchservice.model.match.enums.SportType;
import com.example.trainingmatchservice.model.training.entity.TrainingSession;
import com.example.trainingmatchservice.model.training.enums.TrainingStatus;
import com.example.trainingmatchservice.model.training.enums.TrainingType;
import com.example.trainingmatchservice.repository.MatchRepository;
import com.example.trainingmatchservice.repository.TrainingSessionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.seed.enabled", havingValue = "true")
public class TrainingMatchDataSeeder implements CommandLineRunner {

    // References (deterministic from upstream seeders):
    //   Teams (player-mgmt): 1=Cairo Eagles FC, 2=Alex Stars BC, 3=Cairo Tennis Club, 4=Nile Volleyball
    //   OuterTeams (player-mgmt): 1=Pyramids FC, 2=Al Ahly, 3=Zamalek SC, 4=Tunis Hoops
    //   HeadCoaches (user-mgmt user_profiles): 4=Football HC, 5=Basketball HC
    private static final long TEAM_FB = 1L;
    private static final long TEAM_BB = 2L;
    private static final long OUTER_PYRAMIDS = 1L;
    private static final long OUTER_AHLY = 2L;
    private static final long OUTER_TUNIS_HOOPS = 4L;
    private static final long HEAD_COACH_FB = 4L;
    private static final long HEAD_COACH_BB = 5L;

    private final TrainingSessionRepository trainingSessionRepository;
    private final MatchRepository matchRepository;

    @Override
    @Transactional
    public void run(String... args) {
        if (trainingSessionRepository.count() > 0 || matchRepository.count() > 0) {
            log.info("[SEED] training-match already has data — skipping.");
            return;
        }
        log.info("[SEED] Seeding training-match-service...");

        LocalDateTime now = LocalDateTime.now();

        saveSession(TEAM_FB, HEAD_COACH_FB, TrainingType.TACTICAL,  TrainingStatus.COMPLETED,
                now.minusDays(2), 90, "Cairo Training Ground - Pitch 1",
                "Defensive shape vs. 4-3-3", "Focused on pressing triggers and back-line cohesion.");
        saveSession(TEAM_FB, HEAD_COACH_FB, TrainingType.FITNESS,   TrainingStatus.SCHEDULED,
                now.plusDays(1), 60, "Cairo Training Ground - Gym",
                "VO2 max + sprint intervals", null);
        saveSession(TEAM_FB, HEAD_COACH_FB, TrainingType.RECOVERY,  TrainingStatus.SCHEDULED,
                now.plusDays(2), 45, "Cairo Training Ground - Pool",
                "Active recovery after league fixture", null);
        saveSession(TEAM_BB, HEAD_COACH_BB, TrainingType.TECHNICAL, TrainingStatus.ONGOING,
                now.minusHours(1), 75, "Alex Indoor Arena",
                "Pick & roll execution drills", null);
        saveSession(TEAM_BB, HEAD_COACH_BB, TrainingType.VIDEO_ANALYSIS, TrainingStatus.SCHEDULED,
                now.plusDays(3), 60, "Alex Stars HQ - Room 2",
                "Opponent breakdown — Tunis Hoops", null);

        // Matches
        saveMatch(TEAM_FB, OUTER_PYRAMIDS, MatchType.LEAGUE,   MatchStatus.FINISHED,  SportType.FOOTBALL,
                "Cairo International Stadium", "Egyptian Premier League", "2024/2025",
                2, 1, now.minusDays(5), now.minusDays(5).plusHours(2), "Ibrahim Nour Eldin", 25_000,
                "Solid win against rivals — Salah brace.");
        saveMatch(TEAM_FB, OUTER_AHLY, MatchType.LEAGUE,       MatchStatus.SCHEDULED, SportType.FOOTBALL,
                "Cairo International Stadium", "Egyptian Premier League", "2024/2025",
                null, null, now.plusDays(7), null, "TBD", null,
                null);
        saveMatch(TEAM_BB, OUTER_TUNIS_HOOPS, MatchType.CUP,   MatchStatus.SCHEDULED, SportType.BASKETBALL,
                "Alex Indoor Arena", "Arab Clubs Cup", "2024/2025",
                null, null, now.plusDays(4), null, "TBD", null, null);

        log.info("[SEED] training-match seeded: sessions={}, matches={}",
                trainingSessionRepository.count(), matchRepository.count());
    }

    private TrainingSession saveSession(long teamId, long headCoachId,
                                        TrainingType type, TrainingStatus status,
                                        LocalDateTime when, int duration, String location,
                                        String objectives, String notes) {
        TrainingSession s = new TrainingSession();
        s.setTeamId(teamId);
        s.setHeadCoachId(headCoachId);
        s.setTrainingType(type);
        s.setStatus(status);
        s.setScheduledDateTime(when);
        s.setDurationMinutes(duration);
        s.setLocation(location);
        s.setObjectives(objectives);
        s.setNotes(notes);
        return trainingSessionRepository.save(s);
    }

    private Match saveMatch(long homeTeamId, long outerTeamId, MatchType type, MatchStatus status,
                            SportType sport, String venue, String competition, String season,
                            Integer homeScore, Integer awayScore,
                            LocalDateTime kickoff, LocalDateTime finish,
                            String referee, Integer attendance, String summary) {
        Match m = new Match();
        m.setHomeTeamId(homeTeamId);
        m.setOuterTeamId(outerTeamId);
        m.setMatchType(type);
        m.setStatus(status);
        m.setSportType(sport);
        m.setVenue(venue);
        m.setCompetition(competition);
        m.setSeason(season);
        m.setHomeTeamScore(homeScore);
        m.setAwayTeamScore(awayScore);
        m.setKickoffTime(kickoff);
        m.setFinishTime(finish);
        m.setReferee(referee);
        m.setAttendance(attendance);
        m.setMatchSummary(summary);
        return matchRepository.save(m);
    }
}
