package io.github.regalpine.agentforge.sdk;

import java.util.Map;
import java.util.Set;

/**
 * Read-only descriptor for an Agent.
 * <p>
 * Provides metadata about an Agent without allowing modification of the runtime definition.
 */
public record AgentDescriptor(
    String id,
    String version,
    String description,
    Set<String> capabilities
) {

    public AgentDescriptor {
        capabilities = capabilities == null ? Set.of() : Set.copyOf(capabilities);
    }
}
