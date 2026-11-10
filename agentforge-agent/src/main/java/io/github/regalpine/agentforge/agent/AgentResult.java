package io.github.regalpine.agentforge.agent;

import java.util.Map;

/**
 * Agent execution result.
 * AF-06 §50: AgentResult.
 *
 * <p>Represents the final outcome of an Agent
 * execution cycle. Only this type is visible
 * to Graph Runtime.
 */
public record AgentResult(
    AgentResultStatus status,
    Map<String, Object> outputs,
    Map<String, Object> metadata
) {

    public AgentResult {
        outputs =
            outputs == null
                ? Map.of()
                : Map.copyOf(outputs);

        metadata =
            metadata == null
                ? Map.of()
                : Map.copyOf(metadata);
    }
}
