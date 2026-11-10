package io.github.regalpine.agentforge.server.health;

import java.util.ArrayList;
import java.util.List;

/**
 * Aggregates health checks into liveness and readiness.
 *
 * <p>G4-2: readiness reflects real dependency status.
 * Liveness only reports process-alive; readiness
 * reports whether all registered checks pass.
 */
public final class HealthAggregator {

    private final List<HealthCheck> checks;

    public HealthAggregator(
        List<HealthCheck> checks
    ) {
        this.checks = List.copyOf(checks);
    }

    /**
     * Liveness: process is alive.
     * Does NOT depend on dependency health.
     */
    public HealthStatus liveness() {
        return HealthStatus.healthy("server");
    }

    /**
     * Readiness: all dependencies are available.
     * Returns DOWN if any check fails.
     */
    public HealthStatus readiness() {
        for (HealthCheck check : checks) {
            HealthStatus status = check.check();

            if (!"UP".equals(status.status())) {
                return status;
            }
        }

        return HealthStatus.healthy("all");
    }

    /**
     * G4-2: Detailed readiness with per-component
     * breakdown. Returns all check results regardless
     * of individual failures.
     */
    public List<HealthStatus> detailedReadiness() {
        List<HealthStatus> results = new ArrayList<>();
        for (HealthCheck check : checks) {
            results.add(check.check());
        }
        return List.copyOf(results);
    }

    /**
     * G4-2: Whether the server is in degraded mode
     * (some but not all checks fail).
     */
    public boolean isDegraded() {
        int failures = 0;
        for (HealthCheck check : checks) {
            if (!"UP".equals(check.check().status())) {
                failures++;
            }
        }
        return failures > 0 && failures < checks.size();
    }
}
