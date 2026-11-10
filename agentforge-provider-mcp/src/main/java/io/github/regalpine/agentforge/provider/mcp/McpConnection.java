package io.github.regalpine.agentforge.provider.mcp;

import java.util.Map;
import java.util.Objects;

/**
 * MCP connection descriptor.
 * AE-10 §8: McpConnection.
 */
public record McpConnection(
    String connectionId,
    String serverId,
    String endpoint,
    McpTransportType transport,
    McpConnectionStatus status,
    String credentialReference,
    Map<String, Object> metadata
) {

    public McpConnection {
        Objects.requireNonNull(connectionId, "connectionId");
        Objects.requireNonNull(serverId, "serverId");
        Objects.requireNonNull(endpoint, "endpoint");
        Objects.requireNonNull(transport, "transport");

        status =
            status == null
                ? McpConnectionStatus.CREATED
                : status;

        metadata =
            metadata == null
                ? Map.of()
                : Map.copyOf(metadata);
    }

    public McpConnection withStatus(
        McpConnectionStatus newStatus
    ) {
        return new McpConnection(
            connectionId, serverId, endpoint,
            transport, newStatus,
            credentialReference, metadata
        );
    }
}
