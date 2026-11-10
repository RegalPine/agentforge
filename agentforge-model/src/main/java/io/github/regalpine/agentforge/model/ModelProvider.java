package io.github.regalpine.agentforge.model;

import io.github.regalpine.agentforge.kernel.EntityId;
import io.github.regalpine.agentforge.kernel.Version;

import java.util.List;

/**
 * Service Provider Interface for model providers.
 * <p>
 * A ModelProvider represents a model service implementation, such as:
 * <ul>
 *   <li>OpenAIProvider</li>
 *   <li>AnthropicProvider</li>
 *   <li>OllamaProvider</li>
 *   <li>VllmProvider</li>
 *   <li>AzureProvider</li>
 * </ul>
 * <p>
 * Model providers are responsible for:
 * <ul>
 *   <li>Managing connections to model services</li>
 *   <li>Translating between AgentForge models and provider-specific protocols</li>
 *   <li>Handling authentication and authorization</li>
 *   <li>Reporting health status</li>
 * </ul>
 */
public interface ModelProvider {

    /**
     * Get the provider identifier.
     *
     * @return the provider ID
     */
    EntityId providerId();

    /**
     * Get the provider version.
     *
     * @return the provider version
     */
    Version version();

    /**
     * Get the list of available models.
     *
     * @return the list of model descriptors
     */
    List<ModelDescriptor> availableModels();

    /**
     * Get a chat model by ID.
     *
     * @param modelId the model identifier
     * @return the chat model
     * @throws ModelUnavailableException if the model is not available
     */
    ChatModel chatModel(EntityId modelId);

    /**
     * Get an embedding model by ID.
     *
     * @param modelId the model identifier
     * @return the embedding model
     * @throws ModelUnavailableException if the model is not available
     */
    EmbeddingModel embeddingModel(EntityId modelId);

    /**
     * Get a rerank model by ID.
     *
     * @param modelId the model identifier
     * @return the rerank model
     * @throws ModelUnavailableException if the model is not available
     */
    RerankModel rerankModel(EntityId modelId);

    /**
     * Get a structured output model by ID.
     *
     * @param modelId the model identifier
     * @return the structured output model
     * @throws ModelUnavailableException if the model is not available
     */
    StructuredOutputModel structuredOutputModel(EntityId modelId);

    /**
     * Get the health status of this provider.
     *
     * @return the health status
     */
    ModelProviderHealth health();

    /**
     * Check if this provider supports the given model.
     *
     * @param modelId the model identifier
     * @return true if the model is supported
     */
    boolean supports(EntityId modelId);

    /**
     * Get the rich provider descriptor with metadata.
     *
     * @return the provider descriptor
     */
    default ProviderDescriptor descriptor() {
        return ProviderDescriptor.of(
            providerId(),
            ProviderType.REMOTE,
            version(),
            providerId().value(),
            "",
            availableModels()
        );
    }
}
