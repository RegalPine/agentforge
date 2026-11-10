package io.github.regalpine.agentforge.provider.mcp;

import java.util.Map;

public record McpServerDescriptor(
    String serverId,
    String protocolVersion,
    Map<String, Object> metadata
) {

    public McpServerDescriptor {
        metadata =
            metadata == null
                ? Map.of()
                : Map.copyOf(metadata);
    }
}
