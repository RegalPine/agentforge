package io.github.regalpine.agentforge.agent;

import java.util.Map;

/**
 * Memory query for Agent Memory retrieval.
 * AF-06 §47: MemoryQuery.
 */
public record MemoryQuery(
    String query,
    Map<String, Object> parameters
) {

    public MemoryQuery {
        parameters =
            parameters == null
                ? Map.of()
                : Map.copyOf(parameters);
    }
}
