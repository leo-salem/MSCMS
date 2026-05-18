package com.example.mlmodelservice.model.enums;

/**
 * Origin of a feature row. New features should bump {@code featureVersion} when changing semantics.
 */
public enum FeatureSource {
    KAFKA,
    SEED,
    MANUAL
}
