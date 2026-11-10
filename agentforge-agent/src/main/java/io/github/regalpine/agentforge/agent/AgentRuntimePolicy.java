package io.github.regalpine.agentforge.agent;

import java.util.Map;

/**
 * Agent runtime policy.
 * AF-06 §16: AgentRuntimePolicy.
 *
 * <p>Defines runtime policies that constrain
 * Agent decision-making behavior.
 */
public record AgentRuntimePolicy(
    long maxIterations,
    long timeoutMillis,
    Map<String, Object> constraints
) {

    public AgentRuntimePolicy {
        constraints =
            constraints == null
                ? Map.of()
                : Map.copyOf(constraints);
    }

    public static AgentRuntimePolicy defaults() {
        return new AgentRuntimePolicy(
            100,
            300_000,
            Map.of()
        );
    }
}
