package io.github.regalpine.agentforge.agent;

import io.github.regalpine.agentforge.capability.CapabilityResult;

/**
 * Action execution port for agent
 * capabilities.
 * G3-3: injects real capability
 * invocation into the agent loop.
 *
 * <p>When the decision engine produces a
 * {@link CapabilityDecision}, the agent
 * loop calls this port to actually invoke
 * the capability and obtain a real result.
 *
 * <p>The result is stored in the
 * observation and capability results map,
 * feeding back into the next decision
 * cycle.
 */
@FunctionalInterface
public interface ActionExecutionPort {

    /**
     * Execute a capability decision
     * and return the result.
     *
     * @param decision the capability
     *                 decision from
     *                 the agent loop
     * @return the capability result
     *         (success, failure, waiting,
     *         or cancelled)
     */
    CapabilityResult execute(
        CapabilityDecision decision
    );

    /**
     * Default no-op port that returns a
     * stub failure. Used when no real port
     * is configured.
     */
    static ActionExecutionPort noOp() {
        return decision ->
            new io.github.regalpine.agentforge.capability.CapabilityFailure(
                "NO_ACTION_PORT",
                "No action execution port configured for: "
                    + decision.capabilityId(),
                false
            );
    }
}
