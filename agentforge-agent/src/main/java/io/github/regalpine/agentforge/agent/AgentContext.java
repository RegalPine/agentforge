package io.github.regalpine.agentforge.agent;

import java.util.List;
import java.util.Map;

/**
 * Agent internal decision context.
 * AF-06 §42-§45: AgentContext.
 *
 * <p>This is the Agent Runtime's local working
 * context, isolated from Graph Execution Context.
 * Contains input, goal, variables, observations,
 * capability results, and runtime metadata.
 */
public record AgentContext(
    Map<String, Object> input,
    String goal,
    Map<String, Object> variables,
    List<AgentObservation> observations,
    Map<String, Object> capabilityResults,
    Map<String, Object> metadata
) {

    public AgentContext {
        input =
            input == null
                ? Map.of()
                : Map.copyOf(input);

        variables =
            variables == null
                ? Map.of()
                : Map.copyOf(variables);

        observations =
            observations == null
                ? List.of()
                : List.copyOf(observations);

        capabilityResults =
            capabilityResults == null
                ? Map.of()
                : Map.copyOf(capabilityResults);

        metadata =
            metadata == null
                ? Map.of()
                : Map.copyOf(metadata);
    }
}
