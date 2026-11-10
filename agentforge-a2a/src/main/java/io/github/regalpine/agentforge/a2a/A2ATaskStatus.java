package io.github.regalpine.agentforge.a2a;

/**
 * A2A task status.
 * AF-ENHANCE-10 §31: Task.
 */
public enum A2ATaskStatus {

    SUBMITTED,

    RUNNING,

    INPUT_REQUIRED,

    COMPLETED,

    FAILED,

    CANCELLED,

    TIMED_OUT;

    /**
     * Check if the task is in a
     * terminal state.
     */
    public boolean isTerminal() {
        return this == COMPLETED
            || this == FAILED
            || this == CANCELLED
            || this == TIMED_OUT;
    }
}
