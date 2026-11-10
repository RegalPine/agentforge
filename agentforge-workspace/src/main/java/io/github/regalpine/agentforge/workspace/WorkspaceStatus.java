package io.github.regalpine.agentforge.workspace;

/**
 * Workspace lifecycle.
 * AF-ENHANCE-07 §19: Workspace
 * Lifecycle.
 *
 * <p>CREATED → INITIALIZED → READY →
 * ACTIVE → FROZEN → ARCHIVED →
 * DELETED.
 */
public enum WorkspaceStatus {

    CREATED,

    INITIALIZED,

    READY,

    ACTIVE,

    FROZEN,

    ARCHIVED,

    DELETED;

    /**
     * Whether this status allows
     * read/write operations.
     */
    public boolean isAccessible() {
        return this == READY
            || this == ACTIVE;
    }

    /**
     * Whether this is a terminal
     * status.
     */
    public boolean isTerminal() {
        return this == DELETED;
    }
}
