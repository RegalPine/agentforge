package io.github.regalpine.agentforge.provider.mcp;

import java.util.List;
import java.util.Optional;

/**
 * MCP server registry.
 * AF-ENHANCE-10 §13: Server Registration.
 */
public interface McpServerRegistry {

    /**
     * Register a server definition.
     */
    void register(
        McpServerDefinition definition
    );

    /**
     * Get a server definition by ID.
     */
    Optional<McpServerDefinition> get(
        McpServerId id
    );

    /**
     * List all registered servers.
     */
    List<McpServerDefinition> list();

    /**
     * Disable a server.
     */
    void disable(McpServerId id);

    /**
     * Revoke a server.
     */
    void revoke(McpServerId id);
}
