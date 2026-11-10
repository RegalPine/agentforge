package io.github.regalpine.agentforge.provider.llm;

import java.util.Map;

public record StructuredDecision(
    String decision,
    String capabilityId,
    String capabilityVersion,
    Map<String, Object> arguments,
    String reasoning
) {

    public StructuredDecision {
        arguments =
            arguments == null
                ? Map.of()
                : Map.copyOf(arguments);
    }
}
