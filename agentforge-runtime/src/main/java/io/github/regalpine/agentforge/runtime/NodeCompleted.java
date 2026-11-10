package io.github.regalpine.agentforge.runtime;

import java.util.Map;

public record NodeCompleted(
    Map<String, Object> output
) implements NodeExecutionResult {

    public NodeCompleted {
        output =
            output == null
                ? Map.of()
                : Map.copyOf(output);
    }
}
