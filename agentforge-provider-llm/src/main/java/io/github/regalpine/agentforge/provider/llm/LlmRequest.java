package io.github.regalpine.agentforge.provider.llm;

import java.util.List;
import java.util.Map;

public record LlmRequest(
    String model,
    List<LlmMessage> messages,
    Map<String, Object> parameters,
    long timeoutMillis
) {

    public LlmRequest {
        messages =
            messages == null
                ? List.of()
                : List.copyOf(messages);

        parameters =
            parameters == null
                ? Map.of()
                : Map.copyOf(parameters);
    }
}
