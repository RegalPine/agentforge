package io.github.regalpine.agentforge.agent;

import java.util.Map;

/**
 * Agent runtime request.
 * AF-06 §7: AgentRuntimeRequest.
 *
 * <p>Entry point for Agent Runtime execution.
 * The agentId and agentVersion are fixed at
 * request time and must not change during execution.
 */
public record AgentRuntimeRequest(
    String agentId,
    String agentVersion,
    Map<String, Object> inputs,
    AgentRuntimeOptions options
) {

    public AgentRuntimeRequest {
        inputs =
            inputs == null
                ? Map.of()
                : Map.copyOf(inputs);
    }
}
