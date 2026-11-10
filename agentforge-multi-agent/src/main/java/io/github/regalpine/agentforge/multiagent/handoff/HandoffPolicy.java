package io.github.regalpine.agentforge.multiagent.handoff;

/**
 * HandoffPolicy — AF-ENHANCE-08 §13.
 *
 * Controls what happens to the caller
 * after a handoff:
 *
 * <p>WAIT: Caller waits for result.
 * TERMINATE: Caller terminates.
 * SUSPEND: Caller is suspended.
 */
public enum HandoffPolicy {

    /**
     * Caller waits for handoff result.
     */
    WAIT,

    /**
     * Caller terminates after handoff.
     */
    TERMINATE,

    /**
     * Caller is suspended.
     */
    SUSPEND
}
