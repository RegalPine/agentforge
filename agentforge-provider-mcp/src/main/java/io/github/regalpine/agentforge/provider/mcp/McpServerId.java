package io.github.regalpine.agentforge.provider.mcp;

import java.util.Objects;

/**
 * MCP server identifier.
 * AF-ENHANCE-10 §7: McpClient.
 */
public record McpServerId(
    String value
) {

    public McpServerId {
        Objects.requireNonNull(
            value, "value"
        );
        if (value.isBlank()) {
            throw new IllegalArgumentException(
                "serverId must not be blank"
            );
        }
    }

    @Override
    public String toString() {
        return value;
    }
}
