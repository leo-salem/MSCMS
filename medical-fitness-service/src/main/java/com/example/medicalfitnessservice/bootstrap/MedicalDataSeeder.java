package com.example.medicalfitnessservice.bootstrap;

import com.example.medicalfitnessservice.model.entity.Diagnosis;
import com.example.medicalfitnessservice.model.entity.Injury;
import com.example.medicalfitnessservice.model.entity.Treatment;
import com.example.medicalfitnessservice.model.enums.InjurySeverity;
import com.example.medicalfitnessservice.model.enums.InjuryStatus;
import com.example.medicalfitnessservice.model.enums.InjuryType;
import com.example.medicalfitnessservice.model.enums.TreatmentStatus;
import com.example.medicalfitnessservice.repository.DiagnosisRepository;
import com.example.medicalfitnessservice.repository.InjuryRepository;
import com.example.medicalfitnessservice.repository.TreatmentRepository;
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
public class MedicalDataSeeder implements CommandLineRunner {

    // Cross-service references (kept here so the file is self-explanatory):
    //   player_profiles ids 12..17 = player1..player6 (user-mgmt insert order)
    //   doctor user_profiles id = 6
    //   team ids in player-mgmt: 1=Cairo Eagles FC
    private static final long PLAYER_3_DEF_ID  = 14L;
    private static final long PLAYER_5_BBALL_ID = 16L;
    private static final long TEAM_FB = 1L;
    private static final long TEAM_BB = 2L;
    private static final long DOCTOR_ID = 6L;
    private static final String PLAYER_3_KC = "00000000-0000-0000-0000-000000000103";
    private static final String PLAYER_5_KC = "00000000-0000-0000-0000-000000000105";
    private static final String DOCTOR_KC   = "00000000-0000-0000-0000-000000000030";

    private final InjuryRepository injuryRepository;
    private final DiagnosisRepository diagnosisRepository;
    private final TreatmentRepository treatmentRepository;

    @Override
    @Transactional
    public void run(String... args) {
        if (injuryRepository.count() > 0) {
            log.info("[SEED] medical-fitness already has data — skipping.");
            return;
        }
        log.info("[SEED] Seeding medical-fitness-service...");

        Injury injury1 = saveInjury(PLAYER_3_DEF_ID, TEAM_FB, InjuryType.LIGAMENT_SPRAIN,
                InjurySeverity.MODERATE, InjuryStatus.RECOVERING,
                "Right ankle", "Sprained right ankle during league fixture vs. Pyramids FC.",
                LocalDate.now().minusDays(10));
        saveDiagnosis(injury1, PLAYER_3_KC, "Grade II lateral ankle ligament sprain",
                "MRI shows partial tear of ATFL; CFL intact.",
                "Rest 14 days, no contact training. Re-evaluate in 1 week.");
        saveTreatment(injury1, PLAYER_3_DEF_ID, "Physiotherapy",
                "Progressive loading + manual therapy + balance work.", TreatmentStatus.IN_PROGRESS,
                LocalDate.now().minusDays(9), null);

        Injury injury2 = saveInjury(PLAYER_5_BBALL_ID, TEAM_BB, InjuryType.MUSCLE_STRAIN,
                InjurySeverity.MINOR, InjuryStatus.RECOVERED,
                "Left hamstring", "Mild hamstring strain during sprint session.",
                LocalDate.now().minusDays(25));
        saveDiagnosis(injury2, PLAYER_5_KC, "Grade I hamstring strain",
                "Clinical examination only. No imaging needed.",
                "Returned to full training after 10 days.");
        saveTreatment(injury2, PLAYER_5_BBALL_ID, "Medication",
                "NSAIDs + targeted stretching protocol.", TreatmentStatus.COMPLETED,
                LocalDate.now().minusDays(24), LocalDate.now().minusDays(14));

        log.info("[SEED] medical-fitness seeded: injuries={}, diagnoses={}, treatments={}",
                injuryRepository.count(), diagnosisRepository.count(), treatmentRepository.count());
    }

    private Injury saveInjury(long playerId, long teamId, InjuryType type, InjurySeverity sev,
                              InjuryStatus status, String bodyPart, String description, LocalDate date) {
        Injury i = new Injury();
        i.setPlayerId(playerId);
        i.setTeamId(teamId);
        i.setInjuryType(type);
        i.setSeverity(sev);
        i.setStatus(status);
        i.setBodyPart(bodyPart);
        i.setDescription(description);
        i.setInjuryDate(date);
        i.setReportedAt(date.atTime(9, 30));
        i.setReportedByDoctorId(DOCTOR_ID);
        return injuryRepository.save(i);
    }

    private Diagnosis saveDiagnosis(Injury injury, String playerKc, String diagnosis,
                                    String notes, String recommendations) {
        Diagnosis d = new Diagnosis();
        d.setInjury(injury);
        d.setPlayerKeycloakId(playerKc);
        d.setDoctorKeycloakId(DOCTOR_KC);
        d.setDiagnosis(diagnosis);
        d.setMedicalNotes(notes);
        d.setRecommendations(recommendations);
        d.setDiagnosedAt(LocalDateTime.now().minusDays(1));
        return diagnosisRepository.save(d);
    }

    private Treatment saveTreatment(Injury injury, long playerId, String type, String description,
                                    TreatmentStatus status, LocalDate start, LocalDate end) {
        Treatment t = new Treatment();
        t.setInjury(injury);
        t.setPlayerId(playerId);
        t.setDoctorId(DOCTOR_ID);
        t.setTreatmentType(type);
        t.setDescription(description);
        t.setStatus(status);
        t.setStartDate(start);
        t.setEndDate(end);
        t.setCreatedAt(LocalDateTime.now().minusDays(1));
        return treatmentRepository.save(t);
    }
}
