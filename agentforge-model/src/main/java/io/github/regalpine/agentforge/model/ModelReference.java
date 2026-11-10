package io.github.regalpine.agentforge.model;

import io.github.regalpine.agentforge.kernel.EntityId;
import io.github.regalpine.agentforge.kernel.Version;

import java.util.Objects;

/**
 * Reference to a model, consisting of provider, model name, and version.
 * <p>
 * Agent definitions should reference models via {@code ModelReference},
 * not by holding direct provider instances. This enables deployment-time
 * model substitution without modifying agent definitions.
 * <p>
 * Example:
 * <pre>
 * provider = "openai"
 * model = "gpt-4"
 * version = "1.0.0"
 * </pre>
 */
public record ModelReference(
    EntityId providerId,
    EntityId modelId,
    Version version
) {

    public ModelReference {
        Objects.requireNonNull(providerId, "providerId");
        Objects.requireNonNull(modelId, "modelId");
        Objects.requireNonNull(version, "version");
    }

    /**
     * Create a model reference from string values.
     *
     * @param provider the provider identifier (e.g., "openai")
     * @param model    the model identifier (e.g., "gpt-4")
     * @param version  the model version (e.g., "1.0.0")
     * @return a new ModelReference
     */
    public static ModelReference of(String provider, String model, String version) {
        return new ModelReference(
            new EntityId(provider),
            new EntityId(model),
            new Version(version)
        );
    }

    /**
     * Create a model reference with "latest" version.
     *
     * @param provider the provider identifier
     * @param model    the model identifier
     * @return a new ModelReference with version "1.0.0"
     */
    public static ModelReference of(String provider, String model) {
        return of(provider, model, "1.0.0");
    }

    @Override
    public String toString() {
        return providerId.value() + ":" + modelId.value() + ":" + version.value();
    }
}
