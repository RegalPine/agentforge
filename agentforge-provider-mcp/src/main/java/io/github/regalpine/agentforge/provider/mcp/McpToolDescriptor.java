package io.github.regalpine.agentforge.provider.mcp;

import java.util.Map;

public record McpToolDescriptor(
    String toolName,
    String description,
    Map<String, Object> inputSchema
) {

    public McpToolDescriptor {
        inputSchema =
            inputSchema == null
                ? Map.of()
                : Map.copyOf(inputSchema);
    }
}
