package io.github.regalpine.agentforge.model;

/**
 * Model health status —
 * AF-ENHANCE-09 §34.
 *
 * <p>Lifecycle of a model's
 * health state:
 *
 * <p>REGISTERED → AVAILABLE
 * → DEGRADED → UNAVAILABLE
 * → DISABLED → REVOKED
 */
public enum ModelHealthStatus {

    /**
     * Model registered but
     * not yet validated.
     */
    REGISTERED,

    /**
     * Model is fully operational.
     */
    AVAILABLE,

    /**
     * Model is operational
     * but with degraded perf.
     */
    DEGRADED,

    /**
     * Model is not available.
     */
    UNAVAILABLE,

    /**
     * Model is disabled by admin.
     */
    DISABLED,

    /**
     * Model is revoked.
     * No new invocations allowed.
     */
    REVOKED;

    /**
     * Check if this status allows
     * model invocation.
     */
    public boolean isInvokable() {
        return this == AVAILABLE
            || this == DEGRADED;
    }

    /**
     * Check if this is a terminal
     * state.
     */
    public boolean isTerminal() {
        return this == REVOKED;
    }
}
