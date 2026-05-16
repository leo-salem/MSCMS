package com.example.playerservice.bootstrap;

import com.example.playerservice.model.entity.OuterTeam;
import com.example.playerservice.model.entity.PlayerContract;
import com.example.playerservice.model.entity.Roster;
import com.example.playerservice.model.entity.Sport;
import com.example.playerservice.model.entity.Team;
import com.example.playerservice.model.enums.SportType;
import com.example.playerservice.repository.OuterTeamRepository;
import com.example.playerservice.repository.PlayerContractRepository;
import com.example.playerservice.repository.RosterRepository;
import com.example.playerservice.repository.SportRepository;
import com.example.playerservice.repository.TeamRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.seed.enabled", havingValue = "true")
public class PlayerDataSeeder implements CommandLineRunner {

    private final SportRepository sportRepository;
    private final TeamRepository teamRepository;
    private final RosterRepository rosterRepository;
    private final PlayerContractRepository contractRepository;
    private final OuterTeamRepository outerTeamRepository;

    @Override
    @Transactional
    public void run(String... args) {
        if (sportRepository.count() > 0) {
            log.info("[SEED] player-management already has data — skipping.");
            return;
        }
        log.info("[SEED] Seeding player-management-service (sports, teams, rosters, contracts)...");

        // Order matters — IDs are referenced by user-mgmt seed (Player.rosterId, etc.)
        Sport football   = saveSport("Football",   SportType.FOOTBALL,   SeedIds.SPORT_MANAGER);
        Sport basketball = saveSport("Basketball", SportType.BASKETBALL, SeedIds.SPORT_MANAGER);
        Sport tennis     = saveSport("Tennis",     SportType.TENNIS,     SeedIds.SPORT_MANAGER);
        Sport volleyball = saveSport("Volleyball", SportType.VOLLEYBALL, SeedIds.SPORT_MANAGER);
        Sport swimming   = saveSport("Swimming",   SportType.SWIMMING,   SeedIds.SPORT_MANAGER);
        Sport handball   = saveSport("Handball",   SportType.HANDBALL,   SeedIds.SPORT_MANAGER);

        Team teamFb = saveTeam("Cairo Eagles FC",   "Egypt", football);
        Team teamBb = saveTeam("Alex Stars BC",     "Egypt", basketball);
        Team teamTn = saveTeam("Cairo Tennis Club", "Egypt", tennis);
        Team teamVb = saveTeam("Nile Volleyball",   "Egypt", volleyball);

        String season = "2024/25";
        saveRoster(SeedIds.PLAYER_1_ID, teamFb, season);
        saveRoster(SeedIds.PLAYER_2_ID, teamFb, season);
        saveRoster(SeedIds.PLAYER_3_ID, teamFb, season);
        saveRoster(SeedIds.PLAYER_4_ID, teamFb, season);
        saveRoster(SeedIds.PLAYER_5_ID, teamBb, season);
        saveRoster(SeedIds.PLAYER_6_ID, teamTn, season);

        LocalDate start = LocalDate.of(2024, 7, 1);
        LocalDate end   = LocalDate.of(2027, 6, 30);
        saveContract(SeedIds.PLAYER_1_STRIKER, start, end,  9_500_000L, 120_000_000L);
        saveContract(SeedIds.PLAYER_2_MID,     start, end,  2_800_000L,  30_000_000L);
        saveContract(SeedIds.PLAYER_3_DEF,     start, end,  1_600_000L,  18_000_000L);
        saveContract(SeedIds.PLAYER_4_GK,      start, end,  1_300_000L,  15_000_000L);
        saveContract(SeedIds.PLAYER_5_BBALL,   start, end,    700_000L,   6_000_000L);
        saveContract(SeedIds.PLAYER_6_TENNIS,  start, end,    420_000L,   4_500_000L);

        // A couple of outer (opponent) teams — useful for match seeding later
        saveOuterTeam("Pyramids FC",    "info@pyramids.eg",   "Egypt");
        saveOuterTeam("Al Ahly",        "contact@ahly.eg",    "Egypt");
        saveOuterTeam("Zamalek SC",     "info@zamalek.eg",    "Egypt");
        saveOuterTeam("Tunis Hoops",    "hoops@tn.tn",        "Tunisia");

        log.info("[SEED] player-management seeded: sports={}, teams={}, rosters={}, contracts={}",
                sportRepository.count(), teamRepository.count(),
                rosterRepository.count(), contractRepository.count());
    }

    private Sport saveSport(String name, SportType type, String sportManagerKeycloakId) {
        Sport s = new Sport();
        s.setName(name);
        s.setSportType(type);
        // sportManagerId expects a Long FK — keep null since user-mgmt PK isn't deterministic here.
        // The sport manager's Keycloak identity is the durable link.
        s.setSportManagerId(null);
        return sportRepository.save(s);
    }

    private Team saveTeam(String name, String country, Sport sport) {
        Team t = new Team();
        t.setName(name);
        t.setCountry(country);
        t.setSport(sport);
        return teamRepository.save(t);
    }

    private Roster saveRoster(long playerId, Team team, String season) {
        Roster r = new Roster();
        r.setPlayerId(playerId);
        r.setSeason(season);
        r.setTeam(team);
        return rosterRepository.save(r);
    }

    private PlayerContract saveContract(String playerKeycloakId, LocalDate start, LocalDate end,
                                        long salary, long releaseClause) {
        PlayerContract c = new PlayerContract();
        c.setPlayerKeycloakId(playerKeycloakId);
        c.setStartDate(start);
        c.setEndDate(end);
        c.setSalary(salary);
        c.setReleaseClause(releaseClause);
        return contractRepository.save(c);
    }

    private OuterTeam saveOuterTeam(String name, String email, String country) {
        OuterTeam t = new OuterTeam();
        t.setName(name);
        t.setEmail(email);
        t.setCountry(country);
        return outerTeamRepository.save(t);
    }
}
