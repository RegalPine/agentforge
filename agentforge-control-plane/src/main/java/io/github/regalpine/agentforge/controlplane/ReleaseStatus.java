package io.github.regalpine.agentforge.controlplane;

import java.time.Instant;
import java.util.Map;

/**
 * Release lifecycle states.
 * AF-ENHANCE-14 §47: Release Lifecycle.
 */
public enum ReleaseStatus {

    CREATED,

    VALIDATING,

    APPROVED,

    DEPLOYING,

    ACTIVE,

    REJECTED,

    FAILED,

    ROLLED_BACK;

    /**
     * Check if release is live.
     */
    public boolean isLive() {
        return this == ACTIVE
            || this == DEPLOYING;
    }

    /**
     * Check if release is in a
     * terminal failure state.
     */
    public boolean isTerminal() {
        return this == REJECTED
            || this == FAILED
            || this == ROLLED_BACK;
    }
}
