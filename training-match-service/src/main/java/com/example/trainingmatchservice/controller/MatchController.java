package com.example.trainingmatchservice.controller;

import com.example.trainingmatchservice.dto.request.MatchRequest;
import com.example.trainingmatchservice.dto.response.MatchResponse;
import com.example.trainingmatchservice.dto.validation.Create;
import com.example.trainingmatchservice.dto.validation.Update;
import com.example.trainingmatchservice.service.MatchService;
import jakarta.validation.constraints.Positive;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/matches")
@Validated
public class MatchController {

    private final MatchService matchService;

    public MatchController(MatchService matchService) {
        this.matchService = matchService;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','TEAM_MANAGER','HEAD_COACH')")
    public ResponseEntity<MatchResponse> createMatch(
            @Validated(Create.class) @RequestBody MatchRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(matchService.createMatch(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','TEAM_MANAGER','HEAD_COACH')")
    public ResponseEntity<MatchResponse> updateMatch(
            @PathVariable @Positive Long id,
            @Validated(Update.class) @RequestBody MatchRequest request) {
        return ResponseEntity.ok(matchService.updateMatch(id, request));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','TEAM_MANAGER','HEAD_COACH','ASSISTANT_COACH','SPORT_MANAGER','PERFORMANCE_ANALYST')")
    public ResponseEntity<MatchResponse> getMatch(@PathVariable @Positive Long id) {
        return ResponseEntity.ok(matchService.getMatchById(id));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','TEAM_MANAGER','HEAD_COACH','ASSISTANT_COACH','SPORT_MANAGER','PERFORMANCE_ANALYST')")
    public ResponseEntity<List<MatchResponse>> getAllMatches() {
        return ResponseEntity.ok(matchService.getAllMatches());
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','TEAM_MANAGER')")
    public ResponseEntity<Void> deleteMatch(@PathVariable @Positive Long id) {
        matchService.deleteMatch(id);
        return ResponseEntity.noContent().build();
    }
}

