package io.github.regalpine.agentforge.multiagent.team;

/**
 * CompletionCriteria —
 * AF-ENHANCE-08 §60.
 *
 * Defines when a team execution
 * is considered complete.
 *
 * <p>ALL: All required agents done.
 * <p>ANY: Any one agent done.
 * <p>QUORUM: N agents done.
 * <p>EXPLICIT: Coordinator decides.
 */
public enum CompletionCriteria {

    /**
     * All required agents must
     * complete.
     */
    ALL,

    /**
     * Any one agent completion
     * is sufficient.
     */
    ANY,

    /**
     * A specified number of agents
     * must complete.
     */
    QUORUM,

    /**
     * Coordinator explicitly
     * ends the execution.
     */
    EXPLICIT
}
