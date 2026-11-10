package io.github.regalpine.agentforge.provider.llm;

import java.util.Map;

public record LlmResponse(
    String content,
    String model,
    Map<String, Object> usage
) {

    public LlmResponse {
        usage =
            usage == null
                ? Map.of()
                : Map.copyOf(usage);
    }
}
