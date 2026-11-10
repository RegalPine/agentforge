package io.github.regalpine.agentforge.controlplane;

import java.time.Instant;

/**
 * Runtime instance health.
 * AF-ENHANCE-14 §30: Runtime Health.
 */
public record RuntimeHealth(
    HealthStatus liveness,
    HealthStatus readiness,
    int capacity,
    Instant lastChecked
) {

    public RuntimeHealth {
        if (liveness == null) {
            liveness = HealthStatus.UNKNOWN;
        }
        if (readiness == null) {
            readiness = HealthStatus.UNKNOWN;
        }
        if (lastChecked == null) {
            lastChecked = Instant.now();
        }
    }

    /**
     * Check if instance is healthy.
     */
    public boolean isHealthy() {
        return liveness == HealthStatus.HEALTHY
            && readiness == HealthStatus.HEALTHY;
    }

    /**
     * Create unknown health.
     */
    public static RuntimeHealth unknown() {
        return new RuntimeHealth(
            HealthStatus.UNKNOWN,
            HealthStatus.UNKNOWN,
            0, Instant.now()
        );
    }
}
