package io.github.regalpine.agentforge.workspace;

/**
 * Artifact lifecycle status.
 * AF-ENHANCE-07 §18: Artifact
 * Lifecycle.
 *
 * <p>CREATED → STAGED → READY →
 * REFERENCED → ARCHIVED → EXPIRED →
 * DELETED.
 *
 * <p>Exceptional:
 * INVALID, CORRUPTED, REVOKED.
 */
public enum ArtifactStatus {

    CREATED,

    STAGED,

    READY,

    REFERENCED,

    ARCHIVED,

    EXPIRED,

    DELETED,

    INVALID,

    CORRUPTED,

    REVOKED;

    /**
     * Whether this artifact can
     * be referenced/exported.
     */
    public boolean isAvailable() {
        return this == READY
            || this == REFERENCED;
    }

    /**
     * Whether this is a terminal
     * status.
     */
    public boolean isTerminal() {
        return this == DELETED
            || this == INVALID
            || this == CORRUPTED
            || this == REVOKED;
    }
}
