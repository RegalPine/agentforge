package io.github.regalpine.agentforge.model;

import io.github.regalpine.agentforge.kernel.EntityId;
import io.github.regalpine.agentforge.kernel.Version;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Rich descriptor for a model provider.
 * <p>
 * Contains provider metadata including supported models, capabilities,
 * credential requirements, and health status. Used for provider discovery
 * and registration.
 */
public record ProviderDescriptor(
    EntityId providerId,
    ProviderType providerType,
    Version version,
    String displayName,
    String description,
    List<ModelDescriptor> supportedModels,
    List<String> credentialTypes,
    Map<String, String> metadata
) {

    public ProviderDescriptor {
        Objects.requireNonNull(providerId, "providerId");
        Objects.requireNonNull(providerType, "providerType");
        Objects.requireNonNull(version, "version");
        Objects.requireNonNull(displayName, "displayName");

        supportedModels = supportedModels == null ? List.of() : List.copyOf(supportedModels);
        credentialTypes = credentialTypes == null ? List.of() : List.copyOf(credentialTypes);
        metadata = metadata == null ? Map.of() : Map.copyOf(metadata);
    }

    /**
     * Create a provider descriptor.
     *
     * @param providerId     the provider identifier
     * @param providerType   the provider type
     * @param version        the provider version
     * @param displayName    the display name
     * @param description    the description
     * @param supportedModels the list of supported models
     * @return a new ProviderDescriptor
     */
    public static ProviderDescriptor of(
        EntityId providerId,
        ProviderType providerType,
        Version version,
        String displayName,
        String description,
        List<ModelDescriptor> supportedModels
    ) {
        return new ProviderDescriptor(providerId, providerType, version, displayName, description, supportedModels, null, null);
    }
}
