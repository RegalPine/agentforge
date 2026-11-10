package io.github.regalpine.agentforge.model;

/**
 * Circuit breaker state —
 * AF-ENHANCE-09 §35.
 *
 * <p>CLOSED → OPEN → HALF_OPEN
 * → CLOSED
 *
 * <p>Must be at Runtime/Provider
 * layer, not Agent/Skill.
 */
public enum CircuitBreakerState {

    /**
     * Normal operation.
     * Requests pass through.
     */
    CLOSED,

    /**
     * Failures exceeded threshold.
     * All requests blocked.
     */
    OPEN,

    /**
     * Testing if service recovered.
     * Limited requests allowed.
     */
    HALF_OPEN;

    /**
     * Check if requests are allowed.
     */
    public boolean allowsRequests() {
        return this == CLOSED
            || this == HALF_OPEN;
    }
}
