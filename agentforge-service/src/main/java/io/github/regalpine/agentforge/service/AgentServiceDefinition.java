package io.github.regalpine.agentforge.service;

import java.util.Map;
import java.util.Objects;

/**
 * Agent Service definition.
 * AF-ENHANCE-11 §8: Service Definition.
 *
 * <p>Stores service configuration:
 * agent reference, metadata,
 * policy. Does NOT store
 * execution state.
 */
public record AgentServiceDefinition(
    AgentServiceId serviceId,
    String agentReference,
    AgentServiceMetadata metadata,
    String policyReference,
    ServiceHealth health,
    Map<String, Object> extensions
) {

    public AgentServiceDefinition {
        Objects.requireNonNull(
            serviceId, "serviceId"
        );
        Objects.requireNonNull(
            agentReference, "agentReference"
        );

        metadata = metadata == null
            ? AgentServiceMetadata.defaults()
            : metadata;
        policyReference = policyReference == null
            ? "" : policyReference;
        health = health == null
            ? ServiceHealth.STARTING : health;
        extensions = extensions == null
            ? Map.of() : Map.copyOf(extensions);
    }

    /**
     * Create a definition.
     */
    public static AgentServiceDefinition of(
        AgentServiceId serviceId,
        String agentReference
    ) {
        return new AgentServiceDefinition(
            serviceId, agentReference,
            AgentServiceMetadata.defaults(),
            "", ServiceHealth.STARTING,
            Map.of()
        );
    }
}
