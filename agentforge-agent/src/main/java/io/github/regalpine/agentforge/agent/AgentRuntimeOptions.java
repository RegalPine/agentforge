package io.github.regalpine.agentforge.agent;

import java.util.Map;

/**
 * Agent runtime options.
 * AF-06 §7: AgentRuntimeOptions.
 *
 * <p>Optional configuration for Agent execution
 * such as timeout, max iterations, etc.
 */
public record AgentRuntimeOptions(
    long maxIterations,
    long timeoutMillis,
    Map<String, Object> properties
) {

    public AgentRuntimeOptions {
        properties =
            properties == null
                ? Map.of()
                : Map.copyOf(properties);
    }

    public static AgentRuntimeOptions defaults() {
        return new AgentRuntimeOptions(
            100,
            300_000,
            Map.of()
        );
    }
}
