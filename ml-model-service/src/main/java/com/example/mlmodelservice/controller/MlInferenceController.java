package com.example.mlmodelservice.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

@Tag(name = "ML inference")
@RestController
@RequestMapping("/ml/inference")
public class MlInferenceController {

    @GetMapping("/health")
    @Operation(summary = "Liveness for orchestration (model wiring is separate)")
    public Map<String, String> health() {
        return Map.of("status", "UP", "model", "not-bound");
    }

    @PostMapping("/predict")
    @Operation(summary = "Mock prediction — replace with ONNX / remote model call")
    public Map<String, Object> predict(@RequestBody Map<String, Object> features) {
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("prediction", "MOCK_HOME_WIN");
        out.put("confidence", 0.71);
        out.put("modelVersion", "stub-0");
        out.put("echoFeatureKeys", features != null ? features.keySet() : null);
        return out;
    }
}
