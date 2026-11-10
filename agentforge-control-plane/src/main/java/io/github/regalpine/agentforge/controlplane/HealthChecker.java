package io.github.regalpine.agentforge.controlplane;

/**
 * Health checker SPI.
 * AF-ENHANCE-14 §31: Health Check.
 */
public interface HealthChecker {

    /**
     * Check liveness of an instance.
     * AF-ENHANCE-14 §33: Liveness.
     */
    HealthStatus liveness(
        RuntimeInstance instance
    );

    /**
     * Check readiness of an instance.
     * AF-ENHANCE-14 §32: Readiness.
     */
    HealthStatus readiness(
        RuntimeInstance instance
    );
}
