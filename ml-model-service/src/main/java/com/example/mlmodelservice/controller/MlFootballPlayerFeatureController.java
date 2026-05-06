package com.example.mlmodelservice.controller;

import com.example.mlmodelservice.dto.MlFootballPlayerFeatureRequest;
import com.example.mlmodelservice.dto.MlFootballPlayerFeatureResponse;
import com.example.mlmodelservice.service.MlFootballPlayerFeatureService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "ML football player features")
@RestController
@RequestMapping("/ml/player-features")
@RequiredArgsConstructor
public class MlFootballPlayerFeatureController {

    private final MlFootballPlayerFeatureService service;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a denormalized player row for ML")
    public MlFootballPlayerFeatureResponse create(@RequestBody MlFootballPlayerFeatureRequest request) {
        return service.create(request);
    }

    @PutMapping("/{id}")
    public MlFootballPlayerFeatureResponse update(@PathVariable Long id, @RequestBody MlFootballPlayerFeatureRequest request) {
        return service.update(id, request);
    }

    @GetMapping("/{id}")
    public MlFootballPlayerFeatureResponse get(@PathVariable Long id) {
        return service.get(id);
    }

    @GetMapping
    public List<MlFootballPlayerFeatureResponse> list() {
        return service.list();
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }
}
