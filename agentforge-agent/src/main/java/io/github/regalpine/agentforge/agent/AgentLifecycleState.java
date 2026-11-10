package io.github.regalpine.agentforge.agent;

import java.util.Map;
import java.util.Set;

/**
 * Agent lifecycle state machine.
 *
 * <p>State machine:
 * <pre>
 * [*] → REGISTERED → STARTING → ACTIVE
 *                       ↓          ↓↑
 *                    FAILED ← DRAINING → STOPPED
 * </pre>
 *
 * <p>Rules:
 * <ul>
 *   <li>Only ACTIVE agents can be invoked</li>
 *   <li>DRAINING agents refuse new invocations
 *       but finish in-flight work</li>
 *   <li>Any state → FAILED on unrecoverable error</li>
 *   <li>STOPPED / FAILED are terminal</li>
 * </ul>
 */
public enum AgentLifecycleState {

    REGISTERED,

    STARTING,

    ACTIVE,

    DRAINING,

    STOPPED,

    FAILED;

    private static final Map<AgentLifecycleState, Set<AgentLifecycleState>>
        TRANSITIONS = Map.of(
            REGISTERED, Set.of(STARTING, FAILED),
            STARTING,   Set.of(ACTIVE, FAILED),
            ACTIVE,     Set.of(DRAINING, STOPPED, FAILED),
            DRAINING,   Set.of(STOPPED, ACTIVE, FAILED),
            STOPPED,    Set.of(REGISTERED, FAILED),
            FAILED,     Set.of(REGISTERED, FAILED)
        );

    /**
     * Check if transition to target is allowed.
     */
    public boolean canTransitionTo(
        AgentLifecycleState target
    ) {
        Set<AgentLifecycleState> allowed =
            TRANSITIONS.get(this);
        return allowed != null
            && allowed.contains(target);
    }

    /**
     * Validate and return the target state.
     *
     * @throws IllegalStateException if
     *   transition is not allowed
     */
    public AgentLifecycleState transitionTo(
        AgentLifecycleState target
    ) {
        if (!canTransitionTo(target)) {
            throw new IllegalStateException(
                "Cannot transition from "
                    + this + " to " + target
            );
        }
        return target;
    }

    /**
     * Whether this state allows invocation.
     * Only ACTIVE agents can be invoked.
     */
    public boolean isRunnable() {
        return this == ACTIVE;
    }

    /**
     * Whether this state is a terminal state.
     */
    public boolean isTerminal() {
        return this == STOPPED
            || this == FAILED;
    }

    /**
     * Whether the agent is draining
     * (refusing new work).
     */
    public boolean isDraining() {
        return this == DRAINING;
    }
}
