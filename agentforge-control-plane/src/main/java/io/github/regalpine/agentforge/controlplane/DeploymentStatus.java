package io.github.regalpine.agentforge.controlplane;

/**
 * Deployment lifecycle states.
 * AF-ENHANCE-14 §6: Deployment.
 */
public enum DeploymentStatus {

    CREATED,

    VALIDATING,

    ACTIVE,

    SCALING,

    UPGRADING,

    ROLLING_BACK,

    DEGRADED,

    STOPPED;

    /**
     * Check if deployment is serving.
     */
    public boolean isAvailable() {
        return this == ACTIVE
            || this == SCALING;
    }

    /**
     * Check if deployment is in a
     * terminal state.
     */
    public boolean isTerminal() {
        return this == STOPPED;
    }

    /**
     * Check if deployment is unhealthy.
     */
    public boolean isDegraded() {
        return this == DEGRADED
            || this == ROLLING_BACK;
    }
}
