package io.github.regalpine.agentforge.agent;

import io.github.regalpine.agentforge.kernel.EntityId;
import io.github.regalpine.agentforge.kernel.Reference;
import io.github.regalpine.agentforge.kernel.Version;

import java.util.Set;

public record AgentDefinition(
    EntityId id,
    Version version,
    String name,
    Set<Reference> capabilities
) implements Agent {

    public AgentDefinition {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException(
                "Agent name cannot be blank"
            );
        }

        capabilities =
            capabilities == null
                ? Set.of()
                : Set.copyOf(capabilities);
    }
}
