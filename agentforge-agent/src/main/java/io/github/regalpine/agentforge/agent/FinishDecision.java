package io.github.regalpine.agentforge.agent;

import java.util.Map;

/**
 * Decision to finish the Agent execution successfully.
 * AF-06 §18: FinishDecision.
 */
public record FinishDecision(
    Map<String, Object> outputs
) implements AgentDecision {

    public FinishDecision {
        outputs =
            outputs == null
                ? Map.of()
                : Map.copyOf(outputs);
    }
}
