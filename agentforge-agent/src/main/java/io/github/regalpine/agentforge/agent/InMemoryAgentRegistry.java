package io.github.regalpine.agentforge.agent;

import io.github.regalpine.agentforge.kernel.Reference;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public final class InMemoryAgentRegistry
    implements AgentRegistry {

    private final Map<Reference, AgentDefinition> agents =
        new ConcurrentHashMap<>();

    @Override
    public void register(AgentDefinition agent) {
        Reference reference =
            new Reference(
                agent.id(),
                agent.version()
            );

        AgentDefinition previous =
            agents.putIfAbsent(reference, agent);

        if (previous != null) {
            throw new IllegalStateException(
                "Agent already registered: " + reference
            );
        }
    }

    @Override
    public Optional<AgentDefinition> find(
        Reference reference
    ) {
        return Optional.ofNullable(
            agents.get(reference)
        );
    }

    @Override
    public List<AgentDefinition> list() {
        return List.copyOf(
            new ArrayList<>(agents.values())
        );
    }

    @Override
    public void unregister(
        Reference reference
    ) {
        agents.remove(reference);
    }
}
