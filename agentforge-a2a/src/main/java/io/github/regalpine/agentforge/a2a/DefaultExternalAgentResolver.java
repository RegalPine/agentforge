package io.github.regalpine.agentforge.a2a;

import java.util.ArrayList;
import java.util.List;

/**
 * Default external agent resolver.
 * AF-ENHANCE-10 §50-51.
 *
 * <p>Resolves agents from the
 * registry using capability
 * matching.
 */
public final class DefaultExternalAgentResolver
    implements ExternalAgentResolver {

    private final ExternalAgentRegistry registry;

    public DefaultExternalAgentResolver(
        ExternalAgentRegistry registry
    ) {
        if (registry == null) {
            throw new IllegalArgumentException(
                "registry must not be null"
            );
        }

        this.registry = registry;
    }

    @Override
    public List<AgentCard> resolve(
        AgentCapabilityQuery query
    ) {
        if (query == null) {
            return List.of();
        }

        List<AgentCard> candidates =
            registry.list();

        List<AgentCard> result =
            new ArrayList<>();

        for (AgentCard card : candidates) {
            // Skip revoked agents
            if (card.trust()
                == ExternalAgentTrust.REVOKED
            ) {
                continue;
            }

            // Match capabilities
            if (query.matches(card)) {
                result.add(card);
            }
        }

        return List.copyOf(result);
    }

    @Override
    public AgentCard resolve(
        ExternalAgentReference reference
    ) {
        if (reference == null) {
            return null;
        }

        // Try to find by agent ID
        // (qualified name)
        String qualifiedName =
            reference.qualifiedName();

        for (AgentCard card : registry.list()) {
            if (card.agentId().equals(
                reference.name()
            )) {
                return card;
            }
        }

        return null;
    }
}
