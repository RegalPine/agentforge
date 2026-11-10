package io.github.regalpine.agentforge.memory;

/**
 * Memory lifecycle states.
 * AF-ENHANCE-06 §53: Memory Lifecycle.
 *
 * <p>Full lifecycle:
 * CANDIDATE → VALIDATING →
 * PENDING_APPROVAL → ACCEPTED →
 * ACTIVE → EXPIRED → ARCHIVED →
 * DELETED.
 *
 * <p>Exceptional:
 * REJECTED, INVALID, CONFLICTED,
 * REVOKED.
 */
public enum MemoryLifecycle {

    CANDIDATE,

    VALIDATING,

    PENDING_APPROVAL,

    ACCEPTED,

    ACTIVE,

    EXPIRED,

    ARCHIVED,

    DELETED,

    REJECTED,

    INVALID,

    CONFLICTED,

    REVOKED;

    /**
     * Whether this state is a
     * terminal (final) state.
     */
    public boolean isTerminal() {
        return this == DELETED
            || this == REJECTED
            || this == INVALID
            || this == REVOKED;
    }

    /**
     * Whether this state allows
     * retrieval.
     */
    public boolean isRetrievable() {
        return this == ACTIVE;
    }

    /**
     * Whether transition to the
     * target state is valid.
     */
    public boolean canTransitionTo(
        MemoryLifecycle target
    ) {
        return switch (this) {
            case CANDIDATE ->
                target == VALIDATING
                    || target == REJECTED
                    || target == INVALID;
            case VALIDATING ->
                target == PENDING_APPROVAL
                    || target == ACCEPTED
                    || target == REJECTED
                    || target == INVALID;
            case PENDING_APPROVAL ->
                target == ACCEPTED
                    || target == REJECTED;
            case ACCEPTED ->
                target == ACTIVE
                    || target == ARCHIVED;
            case ACTIVE ->
                target == EXPIRED
                    || target == ARCHIVED
                    || target == CONFLICTED
                    || target == REVOKED;
            case EXPIRED ->
                target == ARCHIVED
                    || target == DELETED;
            case ARCHIVED ->
                target == DELETED;
            case CONFLICTED ->
                target == ACTIVE
                    || target == REJECTED;
            default -> false;
        };
    }
}
