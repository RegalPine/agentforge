package io.github.regalpine.agentforge.model;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

/**
 * Model health info —
 * AF-ENHANCE-09 §34.
 *
 * <p>Records the health check
 * result for a specific model:
 * status, latency, reason.
 */
public record ModelHealth(
    String modelId,
    String providerId,
    ModelHealthStatus status,
    Instant checkedAt,
    Duration latency,
    String reason
) {

    public ModelHealth {
        Objects.requireNonNull(
            modelId, "modelId"
        );
        Objects.requireNonNull(
            providerId, "providerId"
        );
        Objects.requireNonNull(
            status, "status"
        );

        checkedAt = checkedAt == null
            ? Instant.now() : checkedAt;
        latency = latency == null
            ? Duration.ZERO : latency;
        reason = reason == null
            ? "" : reason;
    }

    /**
     * Create a healthy model health.
     */
    public static ModelHealth healthy(
        String modelId,
        String providerId
    ) {
        return new ModelHealth(
            modelId, providerId,
            ModelHealthStatus.AVAILABLE,
            Instant.now(),
            Duration.ZERO, ""
        );
    }

    /**
     * Create an unhealthy health.
     */
    public static ModelHealth unhealthy(
        String modelId,
        String providerId,
        String reason
    ) {
        return new ModelHealth(
            modelId, providerId,
            ModelHealthStatus.UNAVAILABLE,
            Instant.now(),
            Duration.ZERO, reason
        );
    }

    /**
     * Check if the model is healthy.
     */
    public boolean isHealthy() {
        return status.isInvokable();
    }
}
