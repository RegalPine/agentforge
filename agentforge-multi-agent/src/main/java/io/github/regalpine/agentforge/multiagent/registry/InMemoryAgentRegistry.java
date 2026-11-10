package io.github.regalpine.agentforge.multiagent.registry;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * InMemoryAgentRegistry —
 * AF-ENHANCE-08 §24.
 *
 * In-memory implementation of
 * AgentRegistry. Thread-safe via
 * ConcurrentHashMap.
 */
public final class InMemoryAgentRegistry
    implements AgentRegistry {

    private final ConcurrentHashMap<
        String, AgentDefinition
    > agents = new ConcurrentHashMap<>();

    @Override
    public void register(
        AgentDefinition definition
    ) {
        if (definition == null) {
            throw new IllegalArgumentException(
                "Definition cannot be null"
            );
        }
        agents.put(
            definition.agentId(),
            definition
        );
    }

    @Override
    public java.util.Optional<AgentDefinition> get(
        String agentId
    ) {
        return java.util.Optional.ofNullable(
            agents.get(agentId)
        );
    }

    @Override
    public List<AgentDefinition> find(
        AgentQuery query
    ) {
        List<AgentDefinition> result =
            new ArrayList<>();

        for (var def : agents.values()) {
            if (matches(def, query)) {
                result.add(def);
                if (result.size()
                    >= query.maxResults()
                ) {
                    break;
                }
            }
        }

        return List.copyOf(result);
    }

    @Override
    public boolean unregister(
        String agentId
    ) {
        return agents.remove(agentId) != null;
    }

    @Override
    public List<AgentDefinition> listAll() {
        return List.copyOf(agents.values());
    }

    /**
     * Get the number of registered
     * agents.
     */
    public int size() {
        return agents.size();
    }

    private boolean matches(
        AgentDefinition def,
        AgentQuery query
    ) {
        // Check required capabilities
        for (String cap
            : query.requiredCapabilities()
        ) {
            if (!def.capabilities().contains(cap)) {
                return false;
            }
        }

        // Check required skills
        for (String skill
            : query.requiredSkills()
        ) {
            if (!def.skills().contains(skill)) {
                return false;
            }
        }

        // Check metadata filter
        for (var entry
            : query.metadataFilter().entrySet()
        ) {
            String val = def.metadata().get(
                entry.getKey()
            );
            if (!entry.getValue().equals(val)) {
                return false;
            }
        }

        return true;
    }
}
