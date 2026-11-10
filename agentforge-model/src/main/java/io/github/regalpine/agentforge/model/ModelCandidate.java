package io.github.regalpine.agentforge.model;

import java.util.Map;
import java.util.Objects;

/**
 * Model candidate —
 * AF-ENHANCE-09 §48.
 *
 * <p>A model being considered
 * during routing. Includes
 * definition, health, weight,
 * priority, score.
 */
public record ModelCandidate(
    ModelDefinition definition,
    ModelHealth health,
    double weight,
    int priority,
    double score,
    String status,
    Map<String, Object> metadata
) {

    public ModelCandidate {
        Objects.requireNonNull(
            definition, "definition"
        );

        health = health == null
            ? ModelHealth.healthy(
                definition.modelId(),
                definition.providerId()
            )
            : health;

        if (weight <= 0) {
            weight = 1.0;
        }
        if (priority <= 0) {
            priority = 50;
        }

        status = status == null
            ? "CANDIDATE" : status;
        metadata = metadata == null
            ? Map.of() : Map.copyOf(metadata);
    }

    /**
     * Create a basic candidate.
     */
    public static ModelCandidate of(
        ModelDefinition definition
    ) {
        return new ModelCandidate(
            definition, null,
            1.0, 50, 0.0,
            null, null
        );
    }

    /**
     * Create with priority.
     */
    public static ModelCandidate of(
        ModelDefinition definition,
        int priority
    ) {
        return new ModelCandidate(
            definition, null,
            1.0, priority, 0.0,
            null, null
        );
    }

    /**
     * Create with weight.
     */
    public static ModelCandidate of(
        ModelDefinition definition,
        double weight
    ) {
        return new ModelCandidate(
            definition, null,
            weight, 50, 0.0,
            null, null
        );
    }

    /**
     * Return a copy with score.
     */
    public ModelCandidate withScore(
        double score
    ) {
        return new ModelCandidate(
            definition, health,
            weight, priority, score,
            status, metadata
        );
    }

    /**
     * Return a copy with status.
     */
    public ModelCandidate withStatus(
        String status
    ) {
        return new ModelCandidate(
            definition, health,
            weight, priority, score,
            status, metadata
        );
    }

    /**
     * Check if this candidate
     * is available.
     */
    public boolean isAvailable() {
        return health.isHealthy();
    }

    /**
     * Get the model ID.
     */
    public String modelId() {
        return definition.modelId();
    }

    /**
     * Get the provider ID.
     */
    public String providerId() {
        return definition.providerId();
    }
}
