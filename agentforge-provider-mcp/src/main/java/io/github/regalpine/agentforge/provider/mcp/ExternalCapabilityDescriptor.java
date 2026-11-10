package io.github.regalpine.agentforge.provider.mcp;

import io.github.regalpine.agentforge.capability.CapabilityType;
import io.github.regalpine.agentforge.capability.SideEffectClassification;

import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * External capability descriptor.
 * AE-10 §15: ExternalCapabilityDescriptor.
 *
 * <p>Used to represent capabilities
 * discovered from external protocols
 * (MCP, OpenAPI, gRPC, etc.) in a
 * normalized form.
 */
public record ExternalCapabilityDescriptor(
    String capabilityId,
    String providerId,
    String externalId,
    String name,
    String description,
    Map<String, Object> inputSchema,
    Map<String, Object> outputSchema,
    Set<String> tags,
    CapabilityType type,
    SideEffectClassification sideEffect,
    CapabilityRegistrationStatus registrationStatus,
    Map<String, Object> metadata
) {

    public ExternalCapabilityDescriptor {
        Objects.requireNonNull(capabilityId, "capabilityId");
        Objects.requireNonNull(providerId, "providerId");
        Objects.requireNonNull(externalId, "externalId");
        Objects.requireNonNull(name, "name");

        inputSchema =
            inputSchema == null
                ? Map.of()
                : Map.copyOf(inputSchema);

        outputSchema =
            outputSchema == null
                ? Map.of()
                : Map.copyOf(outputSchema);

        tags =
            tags == null
                ? Set.of()
                : Set.copyOf(tags);

        type =
            type == null
                ? CapabilityType.TOOL
                : type;

        sideEffect =
            sideEffect == null
                ? SideEffectClassification.READ_ONLY
                : sideEffect;

        registrationStatus =
            registrationStatus == null
                ? CapabilityRegistrationStatus.DISCOVERED
                : registrationStatus;

        metadata =
            metadata == null
                ? Map.of()
                : Map.copyOf(metadata);
    }

    /**
     * Build the recommended capability
     * identity URI:
     * mcp://serverId/externalId@version
     */
    public String identityUri() {
        return "mcp://"
            + providerId + "/"
            + externalId;
    }
}
