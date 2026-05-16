package com.example.usermanagementservice.bootstrap;

import com.example.usermanagementservice.model.entity.*;
import com.example.usermanagementservice.model.entity.staff.*;
import com.example.usermanagementservice.model.enums.Gender;
import com.example.usermanagementservice.model.enums.Position;
import com.example.usermanagementservice.model.enums.Role;
import com.example.usermanagementservice.model.enums.StaffRole;
import com.example.usermanagementservice.model.enums.StatusOfPlayer;
import com.example.usermanagementservice.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
@Order(20)
@ConditionalOnProperty(name = "app.seed.enabled", havingValue = "true")
public class UserDataSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PlayerRepository playerRepository;
    private final HeadCoachRepository headCoachRepository;
    private final DoctorRepository doctorRepository;
    private final PhysiotherapistRepository physiotherapistRepository;
    private final FitnessCoachRepository fitnessCoachRepository;
    private final TeamManagerRepository teamManagerRepository;
    private final SportManagerRepository sportManagerRepository;
    private final ScoutRepository scoutRepository;
    private final SponsorRepository sponsorRepository;
    private final FanRepository fanRepository;

    @Override
    @Transactional
    public void run(String... args) {
        long existing = userRepository.count();
        if (existing > 1) {
            log.info("[SEED] user-management already has {} users — skipping demo seed.", existing);
            return;
        }
        log.info("[SEED] Seeding demo users into user-management-service...");

        // SEED CONTRACT — these references stay in sync with player-mgmt seed order:
        //   Sports:    1=Football, 2=Basketball, 3=Tennis, 4=Volleyball, 5=Swimming, 6=Handball
        //   Teams:     1=Cairo Eagles FC (FB), 2=Alex Stars BC (BB), 3=Cairo Tennis Club, 4=Nile VB
        //   Rosters:   one per player in player-mgmt insert order → 1..6 line up with player1..player6
        //   Contracts: same — 1..6 line up with player1..player6
        SportManager sportManager = saveSportManager();
        TeamManager teamManager = saveTeamManager(sportManager);

        saveHeadCoach(SeedIds.HEAD_COACH_FB, "headcoach", "Hassan", "Coach",
                "headcoach@mscms.com", "+201000000020", 42, Gender.MALE, "Cairo",
                1L, 1L, teamManager, 10, "UEFA Pro");
        saveHeadCoach(SeedIds.HEAD_COACH_BB, "headcoach2", "Karim", "BasketCoach",
                "headcoach2@mscms.com", "+201000000021", 39, Gender.MALE, "Alexandria",
                2L, 2L, teamManager, 8, "FIBA Level 3");

        saveDoctor(teamManager);
        savePhysio(teamManager);
        saveFitnessCoach(teamManager);

        saveScout();
        saveSponsor();
        saveFan();

        // Football players → team 1 / sport 1
        savePlayer(SeedIds.PLAYER_1_STRIKER, "player1", "Mohamed", "Salah", "player1@mscms.com",
                "+201000000101", 31, Gender.MALE, "Cairo",
                LocalDate.of(1992, 6, 15), "Egyptian", Position.STRIKER, 90_000_000L, 10, 1L, 1L);
        savePlayer(SeedIds.PLAYER_2_MID, "player2", "Ahmed", "Hegazy", "player2@mscms.com",
                "+201000000102", 28, Gender.MALE, "Cairo",
                LocalDate.of(1996, 3, 22), "Egyptian", Position.CENTRAL_MID, 25_000_000L, 8, 2L, 2L);
        savePlayer(SeedIds.PLAYER_3_DEF, "player3", "Omar", "Defender", "player3@mscms.com",
                "+201000000103", 26, Gender.MALE, "Giza",
                LocalDate.of(1998, 11, 5), "Egyptian", Position.CENTER_BACK, 15_000_000L, 4, 3L, 3L);
        savePlayer(SeedIds.PLAYER_4_GK, "player4", "Mahmoud", "Keeper", "player4@mscms.com",
                "+201000000104", 29, Gender.MALE, "Cairo",
                LocalDate.of(1995, 1, 9), "Egyptian", Position.GOALKEEPER, 12_000_000L, 1, 4L, 4L);

        // Basketball → team 2 / sport 2
        savePlayer(SeedIds.PLAYER_5_BBALL, "player5", "Yousef", "Basket", "player5@mscms.com",
                "+201000000105", 24, Gender.MALE, "Alexandria",
                LocalDate.of(2000, 9, 12), "Egyptian", Position.POINT_GUARD, 5_000_000L, 7, 5L, 5L);

        // Tennis → team 3 / sport 3
        savePlayer(SeedIds.PLAYER_6_TENNIS, "player6", "Layla", "Tennis", "player6@mscms.com",
                "+201000000106", 22, Gender.FEMALE, "Cairo",
                LocalDate.of(2002, 4, 18), "Egyptian", Position.SINGLES_PLAYER, 3_000_000L, null, 6L, 6L);

        log.info("[SEED] user-management seeded: total users = {}", userRepository.count());
    }

    // ─── helpers ────────────────────────────────────────────────────────────

    private SportManager saveSportManager() {
        SportManager sm = new SportManager();
        applyCommon(sm, SeedIds.SPORT_MANAGER, "sportmanager", "Samir", "Manager",
                "sportmanager@mscms.com", "+201000000010", 45, Gender.MALE, "Cairo", Role.SPORT_MANGER);
        sm.setSportId(1L);
        sm.setCanManageAllTeams(true);
        return sportManagerRepository.save(sm);
    }

    private TeamManager saveTeamManager(SportManager sportManager) {
        TeamManager tm = new TeamManager();
        applyCommon(tm, SeedIds.TEAM_MANAGER, "teammanager", "Tarek", "Manager",
                "teammanager@mscms.com", "+201000000011", 40, Gender.MALE, "Cairo", Role.TEAM_MANGER);
        tm.setTeamId(1L);
        tm.setCanManageAllStaffMembers(true);
        tm.setSportManager(sportManager);
        return teamManagerRepository.save(tm);
    }

    private HeadCoach saveHeadCoach(String keycloakId, String username, String first, String last,
                                    String email, String phone, int age, Gender gender, String address,
                                    Long sportId, Long teamId, TeamManager tm,
                                    int yearsExp, String licenseLevel) {
        HeadCoach hc = new HeadCoach();
        applyCommon(hc, keycloakId, username, first, last, email, phone, age, gender, address, Role.STAFF);
        hc.setSportId(sportId);
        hc.setTeamId(teamId);
        hc.setStaffrole(StaffRole.HEAD_COACH);
        hc.setTeamManager(tm);
        hc.setYearsOfExperience(yearsExp);
        hc.setCoachingLicenseLevel(licenseLevel);
        hc.setPreManagedTeams(List.of("Demo FC", "Sample United"));
        return headCoachRepository.save(hc);
    }

    private Doctor saveDoctor(TeamManager tm) {
        Doctor d = new Doctor();
        applyCommon(d, SeedIds.DOCTOR, "doctor", "Dalia", "Doctor",
                "doctor@mscms.com", "+201000000030", 38, Gender.FEMALE, "Cairo", Role.STAFF);
        d.setSportId(1L);
        d.setTeamId(1L);
        d.setStaffrole(StaffRole.TEAM_DOCTOR);
        d.setTeamManager(tm);
        d.setSpecialization("Orthopedics");
        return doctorRepository.save(d);
    }

    private Physiotherapist savePhysio(TeamManager tm) {
        Physiotherapist p = new Physiotherapist();
        applyCommon(p, SeedIds.PHYSIO, "physio", "Pierre", "Physio",
                "physio@mscms.com", "+201000000031", 34, Gender.MALE, "Cairo", Role.STAFF);
        p.setSportId(1L);
        p.setTeamId(1L);
        p.setStaffrole(StaffRole.PHYSIOTHERAPIST);
        p.setTeamManager(tm);
        p.setYearsExperience(7);
        return physiotherapistRepository.save(p);
    }

    private FitnessCoach saveFitnessCoach(TeamManager tm) {
        FitnessCoach fc = new FitnessCoach();
        applyCommon(fc, SeedIds.FITNESS_COACH, "fitness", "Fadi", "Fitness",
                "fitness@mscms.com", "+201000000032", 36, Gender.MALE, "Cairo", Role.STAFF);
        fc.setSportId(1L);
        fc.setTeamId(1L);
        fc.setStaffrole(StaffRole.FITNESS_COACH);
        fc.setTeamManager(tm);
        return fitnessCoachRepository.save(fc);
    }

    private Scout saveScout() {
        Scout s = new Scout();
        applyCommon(s, SeedIds.SCOUT, "scout", "Sami", "Scout",
                "scout@mscms.com", "+201000000040", 41, Gender.MALE, "Cairo", Role.SCOUT);
        s.setRegion("North Africa");
        s.setOrganizationName("MSCMS Scouting Network");
        return scoutRepository.save(s);
    }

    private Sponsor saveSponsor() {
        Sponsor sp = new Sponsor();
        applyCommon(sp, SeedIds.SPONSOR, "sponsor", "Sara", "Sponsor",
                "sponsor@mscms.com", "+201000000041", 44, Gender.FEMALE, "Cairo", Role.SPONSOR);
        sp.setCompanyName("Demo Brands Inc.");
        return sponsorRepository.save(sp);
    }

    private Fan saveFan() {
        Fan f = new Fan();
        applyCommon(f, SeedIds.FAN, "fan", "Farid", "Fan",
                "fan@mscms.com", "+201000000042", 27, Gender.MALE, "Cairo", Role.FAN);
        f.setDisplayName("Farid_The_Fan");
        f.setFavoriteTeamId(1L);
        return fanRepository.save(f);
    }

    private Player savePlayer(String keycloakId, String username, String first, String last,
                              String email, String phone, int age, Gender gender, String address,
                              LocalDate dob, String nationality, Position position,
                              Long marketValue, Integer kit, Long rosterId, Long contractId) {
        Player p = new Player();
        applyCommon(p, keycloakId, username, first, last, email, phone, age, gender, address, Role.PLAYER);
        p.setDateOfBirth(dob);
        p.setNationality(nationality);
        p.setPreferredPosition(position);
        p.setMarketValue(marketValue);
        p.setKitNumber(kit);
        p.setRosterId(rosterId);
        p.setContractId(contractId);
        p.setStatus(StatusOfPlayer.AVAILABLE);
        return playerRepository.save(p);
    }

    private void applyCommon(User u, String keycloakId, String username, String first, String last,
                             String email, String phone, int age, Gender gender, String address, Role role) {
        u.setKeycloakId(keycloakId);
        u.setUsername(username);
        u.setFirstName(first);
        u.setLastName(last);
        u.setEmail(email);
        u.setPhone(phone);
        u.setAge(age);
        u.setGender(gender);
        u.setAddress(address);
        u.setRole(role);
    }
}
