package io.github.regalpine.agentforge.provider.mcp;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory MCP server registry.
 * AF-ENHANCE-10 §13.
 */
public final class InMemoryMcpServerRegistry
    implements McpServerRegistry {

    private final Map<McpServerId, McpServerDefinition>
        definitions = new ConcurrentHashMap<>();

    private final Map<McpServerId, McpServerTrust>
        trustOverrides = new ConcurrentHashMap<>();

    @Override
    public void register(
        McpServerDefinition definition
    ) {
        if (definition == null) {
            throw new IllegalArgumentException(
                "definition must not be null"
            );
        }

        definitions.put(
            definition.serverId(),
            definition
        );
    }

    @Override
    public Optional<McpServerDefinition> get(
        McpServerId id
    ) {
        if (id == null) {
            return Optional.empty();
        }

        McpServerDefinition def =
            definitions.get(id);

        if (def == null) {
            return Optional.empty();
        }

        // Apply trust override
        McpServerTrust override =
            trustOverrides.get(id);

        if (override != null
            && override != def.trust()
        ) {
            return Optional.of(
                new McpServerDefinition(
                    def.serverId(),
                    def.endpoint(),
                    def.transport(),
                    def.credentialReference(),
                    override,
                    def.policyReference(),
                    def.metadata()
                )
            );
        }

        return Optional.of(def);
    }

    @Override
    public List<McpServerDefinition> list() {
        return List.copyOf(
            definitions.values()
        );
    }

    @Override
    public void disable(McpServerId id) {
        trustOverrides.put(
            id, McpServerTrust.REVOKED
        );
    }

    @Override
    public void revoke(McpServerId id) {
        definitions.remove(id);
        trustOverrides.remove(id);
    }
}
