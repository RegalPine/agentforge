package io.github.regalpine.agentforge.hardening;

import java.util.Objects;

/**
 * TCK test result.
 * AF-RELEASE-02 §5: four states —
 * PASS, FAIL, BLOCKED, NOT_APPLICABLE.
 *
 * <p>PARTIAL_PASS is explicitly forbidden.
 */
public enum TckResult {

    /**
     * Test passed.
     */
    PASS,

    /**
     * Test failed.
     */
    FAIL,

    /**
     * Test blocked by dependency.
     */
    BLOCKED,

    /**
     * Not applicable to this config.
     */
    NOT_APPLICABLE;

    /**
     * Whether this result counts as
     * passing the release gate.
     * Only PASS qualifies.
     */
    public boolean isPassing() {
        return this == PASS;
    }

    /**
     * Whether this result blocks release.
     * FAIL and BLOCKED block release.
     */
    public boolean isBlocking() {
        return this == FAIL || this == BLOCKED;
    }
}
