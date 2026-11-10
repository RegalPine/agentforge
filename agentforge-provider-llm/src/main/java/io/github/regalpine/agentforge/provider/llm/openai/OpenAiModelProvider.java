package io.github.regalpine.agentforge.provider.llm.openai;

import io.github.regalpine.agentforge.kernel.EntityId;
import io.github.regalpine.agentforge.kernel.Version;
import io.github.regalpine.agentforge.model.*;

import java.util.*;

/**
 * OpenAI-compatible ModelProvider implementation.
 * <p>
 * Provides ChatModel and StructuredOutputModel capabilities
 * through OpenAI-compatible APIs.
 */
public class OpenAiModelProvider implements ModelProvider {

    private final EntityId providerId;
    private final Version version;
    private final OpenAiConfig config;
    private final List<ModelDescriptor> models;
    private final Map<String, OpenAiChatModel> chatModels = new HashMap<>();

    public OpenAiModelProvider(OpenAiConfig config, List<String> modelIds) {
        this.providerId = new EntityId("openai");
        this.version = new Version("1.0.0");
        this.config = Objects.requireNonNull(config, "config");
        this.models = new ArrayList<>();

        for (String modelId : modelIds) {
            ModelDescriptor descriptor = ModelDescriptor.of(
                new EntityId(modelId),
                providerId,
                version,
                modelId,
                "OpenAI-compatible model: " + modelId,
                ModelCapabilities.builder()
                    .chat(true)
                    .toolCalling(true)
                    .structuredOutput(true)
                    .streaming(false)
                    .vision(false)
                    .build()
            );
            models.add(descriptor);
            chatModels.put(modelId, new OpenAiChatModel(config, modelId));
        }
    }

    /**
     * Create an OpenAI provider with a single model.
     *
     * @param config  the configuration
     * @param modelId the model ID
     * @return a new OpenAiModelProvider
     */
    public static OpenAiModelProvider of(OpenAiConfig config, String modelId) {
        return new OpenAiModelProvider(config, List.of(modelId));
    }

    /**
     * Create an OpenAI provider with common models.
     *
     * @param config the configuration
     * @return a new OpenAiModelProvider
     */
    public static OpenAiModelProvider standard(OpenAiConfig config) {
        return new OpenAiModelProvider(config, List.of("gpt-4", "gpt-4-turbo", "gpt-3.5-turbo"));
    }

    @Override
    public EntityId providerId() {
        return providerId;
    }

    @Override
    public Version version() {
        return version;
    }

    @Override
    public List<ModelDescriptor> availableModels() {
        return List.copyOf(models);
    }

    @Override
    public ChatModel chatModel(EntityId modelId) {
        OpenAiChatModel model = chatModels.get(modelId.value());
        if (model == null) {
            throw new ModelUnavailableException(
                "Model not available: " + modelId,
                modelId.value(),
                providerId.value()
            );
        }
        return model;
    }

    @Override
    public EmbeddingModel embeddingModel(EntityId modelId) {
        throw new ModelUnavailableException(
            "Embedding not supported by OpenAI provider yet",
            modelId.value(),
            providerId.value()
        );
    }

    @Override
    public RerankModel rerankModel(EntityId modelId) {
        throw new ModelUnavailableException(
            "Rerank not supported by OpenAI provider",
            modelId.value(),
            providerId.value()
        );
    }

    @Override
    public StructuredOutputModel structuredOutputModel(EntityId modelId) {
        // ChatModel also implements structured output via response_format
        ChatModel chat = chatModel(modelId);
        return new OpenAiStructuredOutputModel(chat);
    }

    @Override
    public ModelProviderHealth health() {
        // Simple health check - try to get a model
        try {
            if (!chatModels.isEmpty()) {
                return ModelProviderHealth.AVAILABLE;
            }
            return ModelProviderHealth.UNKNOWN;
        } catch (Exception e) {
            return ModelProviderHealth.DEGRADED;
        }
    }

    @Override
    public boolean supports(EntityId modelId) {
        return chatModels.containsKey(modelId.value());
    }
}
