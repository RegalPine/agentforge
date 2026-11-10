package io.github.regalpine.agentforge.a2a;

import java.util.Objects;
import java.util.Set;

/**
 * Agent capability query.
 * AF-ENHANCE-10 §47-48: Capability
 * Negotiation / Matching.
 *
 * <p>Used to find external agents
 * that match required capabilities.
 */
public record AgentCapabilityQuery(
    Set<String> requiredCapabilities,
    Set<String> preferredCapabilities,
    String region,
    ExternalAgentTrust minTrust
) {

    public AgentCapabilityQuery {
        requiredCapabilities =
            requiredCapabilities == null
                ? Set.of()
                : Set.copyOf(requiredCapabilities);
        preferredCapabilities =
            preferredCapabilities == null
                ? Set.of()
                : Set.copyOf(preferredCapabilities);
        region = region == null
            ? "" : region;
        minTrust = minTrust == null
            ? ExternalAgentTrust.UNTRUSTED
            : minTrust;
    }

    /**
     * Create a query for specific
     * capabilities.
     */
    public static AgentCapabilityQuery of(
        Set<String> capabilities
    ) {
        return new AgentCapabilityQuery(
            capabilities, Set.of(),
            null, null
        );
    }

    /**
     * Check if an agent card matches
     * this query.
     */
    public boolean matches(AgentCard card) {
        if (card == null) {
            return false;
        }

        // All required capabilities
        // must be present
        for (String required :
            requiredCapabilities
        ) {
            if (!card.hasCapability(
                required)
            ) {
                return false;
            }
        }

        return true;
    }
}
