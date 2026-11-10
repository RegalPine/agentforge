package io.github.regalpine.agentforge.model;

import io.github.regalpine.agentforge.kernel.EntityId;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Registry for model providers and models.
 * <p>
 * The ModelRegistry is responsible for:
 * <ul>
 *   <li>Registering and unregistering model providers</li>
 *   <li>Resolving model references to model instances</li>
 *   <li>Listing available models and providers</li>
 * </ul>
 * <p>
 * The resolution chain is:
 * <pre>
 * ModelReference → ModelRegistry → ModelProvider → Model
 * </pre>
 */
public interface ModelRegistry {

    /**
     * Register a model provider.
     *
     * @param provider the provider to register
     * @throws IllegalStateException if a provider with the same ID is already registered
     */
    void register(ModelProvider provider);

    /**
     * Unregister a model provider.
     *
     * @param providerId the provider ID to unregister
     * @return true if the provider was unregistered, false if not found
     */
    boolean unregister(EntityId providerId);

    /**
     * Get a model provider by ID.
     *
     * @param providerId the provider ID
     * @return the provider, or empty if not found
     */
    Optional<ModelProvider> getProvider(EntityId providerId);

    /**
     * Get all registered providers.
     *
     * @return the list of providers
     */
    List<ModelProvider> providers();

    /**
     * Resolve a model reference to a chat model.
     *
     * @param reference the model reference
     * @return the chat model
     * @throws ModelUnavailableException if the model cannot be resolved
     */
    ChatModel resolveChatModel(ModelReference reference);

    /**
     * Resolve a model reference to an embedding model.
     *
     * @param reference the model reference
     * @return the embedding model
     * @throws ModelUnavailableException if the model cannot be resolved
     */
    EmbeddingModel resolveEmbeddingModel(ModelReference reference);

    /**
     * Resolve a model reference to a rerank model.
     *
     * @param reference the model reference
     * @return the rerank model
     * @throws ModelUnavailableException if the model cannot be resolved
     */
    RerankModel resolveRerankModel(ModelReference reference);

    /**
     * Resolve a model reference to a structured output model.
     *
     * @param reference the model reference
     * @return the structured output model
     * @throws ModelUnavailableException if the model cannot be resolved
     */
    StructuredOutputModel resolveStructuredOutputModel(ModelReference reference);

    /**
     * List all available model descriptors.
     *
     * @return the list of model descriptors
     */
    List<ModelDescriptor> listModels();

    /**
     * Find a model descriptor by reference.
     *
     * @param reference the model reference
     * @return the model descriptor, or empty if not found
     */
    Optional<ModelDescriptor> findModel(ModelReference reference);

    /**
     * List all provider descriptors with metadata.
     *
     * @return the list of provider descriptors
     */
    default List<ProviderDescriptor> listProviderDescriptors() {
        return providers().stream()
            .map(ModelProvider::descriptor)
            .toList();
    }

    /**
     * Get a health summary of all registered providers.
     *
     * @return a map of provider ID to health status
     */
    default Map<String, ModelProviderHealth> healthSummary() {
        java.util.Map<String, ModelProviderHealth> summary = new java.util.LinkedHashMap<>();
        for (ModelProvider provider : providers()) {
            summary.put(provider.providerId().value(), provider.health());
        }
        return Map.copyOf(summary);
    }
}
