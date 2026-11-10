package io.github.regalpine.agentforge.model;

import java.util.Map;
import java.util.Objects;

/**
 * Model definition — AF-ENHANCE-09 §6.
 *
 * <p>Describes the static capabilities
 * of a model: identity, display name,
 * capabilities, limits, pricing,
 * metadata.
 *
 * <p>ModelDefinition is what a Provider
 * publishes. Model is what Runtime
 * creates from it.
 */
public record ModelDefinition(
    String modelId,
    String providerId,
    String displayName,
    String description,
    String version,
    ModelCapabilities capabilities,
    ModelLimits limits,
    ModelPricing pricing,
    Map<String, Object> metadata
) {

    public ModelDefinition {
        Objects.requireNonNull(
            modelId, "modelId"
        );
        Objects.requireNonNull(
            providerId, "providerId"
        );
        Objects.requireNonNull(
            capabilities, "capabilities"
        );

        displayName = displayName == null
            ? modelId : displayName;
        description = description == null
            ? "" : description;
        version = version == null
            ? "1.0.0" : version;
        limits = limits == null
            ? ModelLimits.defaults() : limits;
        metadata = metadata == null
            ? Map.of() : Map.copyOf(metadata);
    }

    /**
     * Create a minimal definition.
     */
    public static ModelDefinition of(
        String modelId,
        String providerId,
        ModelCapabilities capabilities
    ) {
        return new ModelDefinition(
            modelId, providerId,
            null, null, null,
            capabilities, null, null, null
        );
    }

    /**
     * Create with limits and pricing.
     */
    public static ModelDefinition of(
        String modelId,
        String providerId,
        ModelCapabilities capabilities,
        ModelLimits limits,
        ModelPricing pricing
    ) {
        return new ModelDefinition(
            modelId, providerId,
            null, null, null,
            capabilities, limits, pricing,
            null
        );
    }

    /**
     * Check if this model supports
     * a specific capability.
     */
    public boolean supports(
        String capability
    ) {
        return switch (capability) {
            case "chat" -> capabilities.chat();
            case "embedding" ->
                capabilities.embedding();
            case "rerank" ->
                capabilities.rerank();
            case "structuredOutput" ->
                capabilities.structuredOutput();
            case "toolCalling" ->
                capabilities.toolCalling();
            case "streaming" ->
                capabilities.streaming();
            case "vision" ->
                capabilities.vision();
            default -> false;
        };
    }
}
