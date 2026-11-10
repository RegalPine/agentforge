package io.github.regalpine.agentforge.evolution;

/**
 * Candidate lifecycle states.
 * AF-ENHANCE-15 §14: Candidate Status.
 */
public enum CandidateStatus {

    CREATED,

    ANALYZING,

    GENERATED,

    VALIDATING,

    EXPERIMENTING,

    REVIEW_REQUIRED,

    APPROVED,

    REJECTED,

    RELEASED,

    EXPIRED;

    /**
     * Check if candidate is active.
     */
    public boolean isActive() {
        return this != REJECTED
            && this != RELEASED
            && this != EXPIRED;
    }

    /**
     * Check if candidate passed.
     */
    public boolean isPassed() {
        return this == APPROVED
            || this == RELEASED;
    }

    /**
     * Check if candidate is in a
     * terminal state.
     */
    public boolean isTerminal() {
        return this == REJECTED
            || this == RELEASED
            || this == EXPIRED;
    }
}
