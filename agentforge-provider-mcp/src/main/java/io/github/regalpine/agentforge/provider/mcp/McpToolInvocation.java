package io.github.regalpine.agentforge.provider.mcp;

import java.util.Map;

public record McpToolInvocation(
    String toolName,
    Map<String, Object> arguments
) {

    public McpToolInvocation {
        arguments =
            arguments == null
                ? Map.of()
                : Map.copyOf(arguments);
    }
}
