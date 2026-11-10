package io.github.regalpine.agentforge.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory service registry.
 * AF-ENHANCE-11 §63.
 */
public final class InMemoryAgentServiceRegistry
    implements AgentServiceRegistry {

    private final Map<AgentServiceId,
        AgentServiceDefinition> definitions
        = new ConcurrentHashMap<>();

    @Override
    public void register(
        AgentServiceDefinition definition
    ) {
        if (definition == null) {
            throw new IllegalArgumentException(
                "definition must not be null"
            );
        }

        definitions.put(
            definition.serviceId(),
            definition
        );
    }

    @Override
    public Optional<AgentServiceDefinition> get(
        AgentServiceId id
    ) {
        if (id == null) {
            return Optional.empty();
        }

        return Optional.ofNullable(
            definitions.get(id)
        );
    }

    @Override
    public List<AgentServiceDefinition> list() {
        return List.copyOf(
            definitions.values()
        );
    }

    @Override
    public void disable(
        AgentServiceId id
    ) {
        var existing = definitions.get(id);

        if (existing != null) {
            definitions.put(
                id,
                new AgentServiceDefinition(
                    existing.serviceId(),
                    existing.agentReference(),
                    existing.metadata(),
                    existing.policyReference(),
                    ServiceHealth.STOPPED,
                    existing.extensions()
                )
            );
        }
    }

    @Override
    public void unregister(
        AgentServiceId id
    ) {
        definitions.remove(id);
    }
}
