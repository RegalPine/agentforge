package io.github.regalpine.agentforge.provider.mcp;

import java.util.Collection;
import java.util.List;

/**
 * Enhanced MCP client adapter.
 * AE-10 §6-10: Connection lifecycle,
 * discovery, invocation.
 */
public interface McpClientAdapter {

    McpServerDescriptor descriptor();

    /**
     * Get the current connection.
     */
    McpConnection connection();

    /**
     * Connect to the MCP server.
     * Transitions: CREATED → CONNECTING → CONNECTED → READY
     */
    McpConnection connect();

    /**
     * Disconnect from the MCP server.
     */
    void disconnect();

    /**
     * Discover available tools.
     * AE-10 §11: MCP Server Discovery.
     */
    List<McpToolDescriptor> discoverTools();

    /**
     * Discover and normalize tools into
     * ExternalCapabilityDescriptors.
     * AE-10 §15-16.
     */
    default Collection<ExternalCapabilityDescriptor> discoverCapabilities() {
        return discoverTools().stream()
            .map(
                tool -> SchemaNormalizer.normalize(
                    tool,
                    descriptor().serverId(),
                    descriptor().serverId()
                )
            )
            .toList();
    }

    /**
     * Invoke a tool.
     */
    McpToolResult invoke(McpToolInvocation invocation);
}
