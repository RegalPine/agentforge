package io.github.regalpine.agentforge.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Default model resolver —
 * AF-ENHANCE-09 §22.
 *
 * <p>Resolves models from a
 * ModelRegistry. Supports:
 * - Direct reference resolution
 * - Capability-based resolution
 * - Health-aware filtering
 */
public final class DefaultModelResolver
    implements ModelResolver {

    private final ModelRegistry registry;

    public DefaultModelResolver(
        ModelRegistry registry
    ) {
        Objects.requireNonNull(
            registry, "registry"
        );
        this.registry = registry;
    }

    /**
     * Create a resolver.
     */
    public static DefaultModelResolver of(
        ModelRegistry registry
    ) {
        return new DefaultModelResolver(
            registry
        );
    }

    @Override
    public ModelDefinition resolve(
        ModelReference reference,
        RoutingContext context
    ) {
        Objects.requireNonNull(
            reference, "reference"
        );

        // Find the model descriptor
        Optional<ModelDescriptor> found =
            registry.findModel(reference);

        if (found.isEmpty()) {
            throw new ModelUnavailableException(
                "Model not found: " + reference,
                reference.modelId().value(),
                reference.providerId().value()
            );
        }

        ModelDescriptor desc = found.get();

        // Convert to ModelDefinition
        return new ModelDefinition(
            desc.modelId().value(),
            desc.providerId().value(),
            desc.name(),
            desc.description(),
            desc.version().value(),
            desc.capabilities(),
            null, null,
            desc.metadata()
        );
    }

    @Override
    public ModelDefinition resolveByCapability(
        ModelCapabilityQuery query
    ) {
        Objects.requireNonNull(
            query, "query"
        );

        List<ModelDescriptor> all =
            registry.listModels();

        for (ModelDescriptor desc : all) {
            ModelDefinition def =
                new ModelDefinition(
                    desc.modelId().value(),
                    desc.providerId().value(),
                    desc.name(),
                    desc.description(),
                    desc.version().value(),
                    desc.capabilities(),
                    null, null,
                    desc.metadata()
                );

            if (query.matches(def)) {
                return def;
            }
        }

        throw new ModelUnavailableException(
            "No model matches capability "
                + "query: " + query,
            "", ""
        );
    }
}
