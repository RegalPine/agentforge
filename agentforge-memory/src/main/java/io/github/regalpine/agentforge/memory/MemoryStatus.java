package io.github.regalpine.agentforge.memory;

/**
 * Memory lifecycle status.
 * AE-08 §13: Memory Status.
 *
 * <p>Lifecycle:
 * CANDIDATE → ACTIVE → UPDATED → EXPIRED → ARCHIVED.
 * Deletion transitions to DELETED.
 */
public enum MemoryStatus {

    CANDIDATE,

    ACTIVE,

    EXPIRED,

    ARCHIVED,

    DELETED
}
