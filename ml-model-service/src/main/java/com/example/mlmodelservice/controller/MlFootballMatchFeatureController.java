package com.example.mlmodelservice.controller;

import com.example.mlmodelservice.dto.MlFootballMatchFeatureRequest;
import com.example.mlmodelservice.dto.MlFootballMatchFeatureResponse;
import com.example.mlmodelservice.service.MlFootballMatchFeatureService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "ML football match features")
@RestController
@RequestMapping("/ml/match-features")
@RequiredArgsConstructor
public class MlFootballMatchFeatureController {

    private final MlFootballMatchFeatureService service;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a denormalized match row for ML")
    public MlFootballMatchFeatureResponse create(@RequestBody MlFootballMatchFeatureRequest request) {
        return service.create(request);
    }

    @PutMapping("/{id}")
    public MlFootballMatchFeatureResponse update(@PathVariable Long id, @RequestBody MlFootballMatchFeatureRequest request) {
        return service.update(id, request);
    }

    @GetMapping("/{id}")
    public MlFootballMatchFeatureResponse get(@PathVariable Long id) {
        return service.get(id);
    }

    @GetMapping
    public List<MlFootballMatchFeatureResponse> list() {
        return service.list();
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }
}
