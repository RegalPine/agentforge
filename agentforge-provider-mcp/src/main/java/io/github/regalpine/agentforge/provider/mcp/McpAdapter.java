package io.github.regalpine.agentforge.provider.mcp;

/**
 * MCP adapter interface.
 * AF-ENHANCE-10 §67: McpAdapter.
 *
 * <p>Extends ProtocolAdapter with
 * MCP-specific connect/expose.
 */
public interface McpAdapter
    extends ProtocolAdapter {

    /**
     * Connect to an MCP server.
     */
    McpClient connect(
        McpServerDefinition definition,
        ProtocolContext context
    );

    /**
     * Expose AgentForge capabilities
     * as an MCP server.
     */
    McpServer expose(
        ProtocolContext context
    );

    @Override
    default String protocol() {
        return "mcp";
    }
}
