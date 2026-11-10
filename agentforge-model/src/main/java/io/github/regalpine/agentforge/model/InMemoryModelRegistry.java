package io.github.regalpine.agentforge.model;

import io.github.regalpine.agentforge.kernel.EntityId;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory implementation of {@link ModelRegistry}.
 * <p>
 * This implementation is suitable for:
 * <ul>
 *   <li>Testing</li>
 *   <li>Development</li>
 *   <li>Single-process deployments</li>
 * </ul>
 * <p>
 * For distributed deployments, use a persistent implementation.
 */
public class InMemoryModelRegistry implements ModelRegistry {

    private final Map<EntityId, ModelProvider> providers = new ConcurrentHashMap<>();

    @Override
    public void register(ModelProvider provider) {
        if (provider == null) {
            throw new IllegalArgumentException("provider cannot be null");
        }

        EntityId providerId = provider.providerId();
        if (providers.containsKey(providerId)) {
            throw new IllegalStateException("Provider already registered: " + providerId);
        }

        providers.put(providerId, provider);
    }

    @Override
    public boolean unregister(EntityId providerId) {
        if (providerId == null) {
            throw new IllegalArgumentException("providerId cannot be null");
        }
        return providers.remove(providerId) != null;
    }

    @Override
    public Optional<ModelProvider> getProvider(EntityId providerId) {
        if (providerId == null) {
            throw new IllegalArgumentException("providerId cannot be null");
        }
        return Optional.ofNullable(providers.get(providerId));
    }

    @Override
    public List<ModelProvider> providers() {
        return new ArrayList<>(providers.values());
    }

    @Override
    public ChatModel resolveChatModel(ModelReference reference) {
        validateReference(reference);

        ModelProvider provider = resolveProvider(reference);
        return provider.chatModel(reference.modelId());
    }

    @Override
    public EmbeddingModel resolveEmbeddingModel(ModelReference reference) {
        validateReference(reference);

        ModelProvider provider = resolveProvider(reference);
        return provider.embeddingModel(reference.modelId());
    }

    @Override
    public RerankModel resolveRerankModel(ModelReference reference) {
        validateReference(reference);

        ModelProvider provider = resolveProvider(reference);
        return provider.rerankModel(reference.modelId());
    }

    @Override
    public StructuredOutputModel resolveStructuredOutputModel(ModelReference reference) {
        validateReference(reference);

        ModelProvider provider = resolveProvider(reference);
        return provider.structuredOutputModel(reference.modelId());
    }

    @Override
    public List<ModelDescriptor> listModels() {
        List<ModelDescriptor> models = new ArrayList<>();
        for (ModelProvider provider : providers.values()) {
            models.addAll(provider.availableModels());
        }
        return models;
    }

    @Override
    public Optional<ModelDescriptor> findModel(ModelReference reference) {
        if (reference == null) {
            return Optional.empty();
        }

        for (ModelProvider provider : providers.values()) {
            if (provider.providerId().equals(reference.providerId())) {
                for (ModelDescriptor model : provider.availableModels()) {
                    if (model.modelId().equals(reference.modelId())) {
                        return Optional.of(model);
                    }
                }
            }
        }
        return Optional.empty();
    }

    private void validateReference(ModelReference reference) {
        if (reference == null) {
            throw new IllegalArgumentException("reference cannot be null");
        }
    }

    private ModelProvider resolveProvider(ModelReference reference) {
        ModelProvider provider = providers.get(reference.providerId());
        if (provider == null) {
            throw new ModelUnavailableException(
                "Provider not found: " + reference.providerId(),
                reference.modelId().value(),
                reference.providerId().value()
            );
        }

        if (!provider.supports(reference.modelId())) {
            throw new ModelUnavailableException(
                "Model not supported by provider: " + reference.modelId() + " in " + reference.providerId(),
                reference.modelId().value(),
                reference.providerId().value()
            );
        }

        return provider;
    }
}
