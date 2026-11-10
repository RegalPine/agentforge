package io.github.regalpine.agentforge.service;

import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Agent Service metadata.
 * AF-ENHANCE-11 §8: Service Definition.
 */
public record AgentServiceMetadata(
    String description,
    Set<String> supportedChannels,
    boolean authenticationRequired,
    String policyReference,
    Map<String, Object> extensions
) {

    public AgentServiceMetadata {
        Objects.requireNonNull(
            supportedChannels,
            "supportedChannels"
        );

        description = description == null
            ? "" : description;
        policyReference = policyReference == null
            ? "" : policyReference;
        extensions = extensions == null
            ? Map.of() : Map.copyOf(extensions);
    }

    /**
     * Create minimal metadata.
     */
    public static AgentServiceMetadata defaults() {
        return new AgentServiceMetadata(
            "", Set.of("HTTP"), false,
            "", Map.of()
        );
    }
}
