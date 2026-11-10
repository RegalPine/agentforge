package io.github.regalpine.agentforge.multiagent.handoff;

import java.util.concurrent.CompletionStage;

/**
 * AgentHandoff — AF-ENHANCE-08 §14.
 *
 * Interface for transferring control
 * from one agent to another.
 *
 * <p>Handoff ≠ Delegation:
 * Delegation: A retains control.
 * Handoff: B becomes the new owner.
 */
public interface AgentHandoff {

    /**
     * Transfer control to target agent.
     *
     * @param targetAgentId
     *     the agent to hand off to
     * @param request
     *     handoff request with context
     * @return
     *     completion stage with result
     */
    CompletionStage<HandoffResult> handoff(
        String targetAgentId,
        HandoffRequest request
    );
}
