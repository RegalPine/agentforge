package io.github.regalpine.agentforge.model;

/**
 * Model lifecycle —
 * AF-ENHANCE-09 §84.
 *
 * <p>Full lifecycle of a model:
 *
 * <p>Normal flow:
 * REGISTERED → VALIDATED →
 * AVAILABLE → IN_USE →
 * DEPRECATED → DISABLED →
 * REVOKED
 *
 * <p>Abnormal:
 * INVALID, UNAVAILABLE
 */
public enum ModelLifecycle {

    /**
     * Model registered.
     */
    REGISTERED,

    /**
     * Model validated.
     */
    VALIDATED,

    /**
     * Model available for use.
     */
    AVAILABLE,

    /**
     * Model actively in use.
     */
    IN_USE,

    /**
     * Model deprecated.
     * Existing refs allowed.
     * New bindings discouraged.
     */
    DEPRECATED,

    /**
     * Model disabled by admin.
     */
    DISABLED,

    /**
     * Model revoked.
     * No new invocations.
     */
    REVOKED,

    /**
     * Model invalid.
     */
    INVALID,

    /**
     * Model unavailable.
     */
    UNAVAILABLE;

    /**
     * Check if this lifecycle state
     * allows invocation.
     */
    public boolean isInvokable() {
        return this == AVAILABLE
            || this == IN_USE;
    }

    /**
     * Check if this is a terminal
     * state.
     */
    public boolean isTerminal() {
        return this == REVOKED
            || this == INVALID;
    }

    /**
     * Check if deprecated but
     * still usable.
     */
    public boolean isDeprecated() {
        return this == DEPRECATED;
    }

    /**
     * Can transition to target?
     */
    public boolean canTransitionTo(
        ModelLifecycle target
    ) {
        return switch (this) {
            case REGISTERED ->
                target == VALIDATED
                    || target == INVALID;
            case VALIDATED ->
                target == AVAILABLE
                    || target == INVALID;
            case AVAILABLE ->
                target == IN_USE
                    || target == DEPRECATED
                    || target == DISABLED
                    || target == UNAVAILABLE;
            case IN_USE ->
                target == DEPRECATED
                    || target == DISABLED
                    || target == UNAVAILABLE;
            case DEPRECATED ->
                target == DISABLED
                    || target == REVOKED;
            case DISABLED ->
                target == AVAILABLE
                    || target == REVOKED;
            case UNAVAILABLE ->
                target == AVAILABLE
                    || target == REVOKED;
            case REVOKED, INVALID ->
                false;
        };
    }
}
