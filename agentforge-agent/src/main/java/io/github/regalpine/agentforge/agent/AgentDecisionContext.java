package io.github.regalpine.agentforge.agent;

import io.github.regalpine.agentforge.agent.Agent;
import io.github.regalpine.agentforge.capability.ProviderDescriptor;

import java.util.List;

/**
 * Decision Engine input context.
 * AF-06 §16: AgentDecisionContext.
 *
 * <p>Contains all information available to
 * the Decision Engine when making a decision:
 * the Agent definition, internal context,
 * available capabilities, and runtime policy.
 */
public record AgentDecisionContext(
    Agent agent,
    AgentContext context,
    List<ProviderDescriptor> capabilities,
    AgentRuntimePolicy policy
) {

    public AgentDecisionContext {
        capabilities =
            capabilities == null
                ? List.of()
                : List.copyOf(capabilities);
    }
}
