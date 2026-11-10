package io.github.regalpine.agentforge.provider.mcp;

import java.util.Map;
import java.util.Objects;

/**
 * MCP server information.
 * AF-ENHANCE-10 §7: serverInfo().
 */
public record McpServerInfo(
    McpServerId serverId,
    String name,
    String version,
    String protocolVersion,
    Map<String, Object> metadata
) {

    public McpServerInfo {
        Objects.requireNonNull(
            serverId, "serverId"
        );
        Objects.requireNonNull(
            name, "name"
        );

        version = version == null
            ? "0.0.0" : version;
        protocolVersion = protocolVersion == null
            ? "2025-06-18" : protocolVersion;
        metadata = metadata == null
            ? Map.of() : Map.copyOf(metadata);
    }
}
