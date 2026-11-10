package io.github.regalpine.agentforge.provider.mcp;

import java.util.Map;

public record McpToolResult(
    boolean success,
    Map<String, Object> output,
    String error
) {

    public McpToolResult {
        output =
            output == null
                ? Map.of()
                : Map.copyOf(output);
    }
}
