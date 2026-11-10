package io.github.regalpine.agentforge.model;

/**
 * Strategy for repairing non-conforming output.
 */
public enum RepairStrategy {
    /**
     * Retry with feedback about what went wrong.
     */
    RETRY_WITH_FEEDBACK,

    /**
     * Attempt to fix the output programmatically.
     */
    PROGRAMMATIC_FIX,

    /**
     * No repair - fail immediately.
     */
    NONE
}
