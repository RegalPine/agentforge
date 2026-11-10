package io.github.regalpine.agentforge.model;

import io.github.regalpine.agentforge.kernel.EntityId;
import io.github.regalpine.agentforge.kernel.Version;

import java.util.Map;
import java.util.Objects;

/**
 * Descriptor for a model available through a provider.
 */
public record ModelDescriptor(
    EntityId modelId,
    EntityId providerId,
    Version version,
    String name,
    String description,
    ModelCapabilities capabilities,
    Map<String, Object> metadata
) {

    public ModelDescriptor {
        Objects.requireNonNull(modelId, "modelId");
        Objects.requireNonNull(providerId, "providerId");
        Objects.requireNonNull(version, "version");
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(capabilities, "capabilities");

        metadata = metadata == null ? Map.of() : Map.copyOf(metadata);
    }

    /**
     * Create a model descriptor.
     *
     * @param modelId     the model identifier
     * @param providerId  the provider identifier
     * @param version     the model version
     * @param name        the display name
     * @param description the description
     * @param capabilities the model capabilities
     * @return a new ModelDescriptor
     */
    public static ModelDescriptor of(
        EntityId modelId,
        EntityId providerId,
        Version version,
        String name,
        String description,
        ModelCapabilities capabilities
    ) {
        return new ModelDescriptor(modelId, providerId, version, name, description, capabilities, null);
    }
}
