package io.github.regalpine.agentforge.provider.openai;

import io.github.regalpine.agentforge.kernel.EntityId;
import io.github.regalpine.agentforge.kernel.Version;
import io.github.regalpine.agentforge.model.*;

import java.util.*;

/**
 * OpenAI ModelProvider implementation.
 * <p>
 * Provides ChatModel (with streaming), EmbeddingModel, and StructuredOutputModel
 * through the OpenAI API.
 */
public class OpenAiProviderModelProvider implements ModelProvider {

    private final EntityId providerId;
    private final Version version;
    private final OpenAiProviderConfig config;
    private final List<ModelDescriptor> models;
    private final Map<String, OpenAiProviderChatModel> chatModels = new HashMap<>();
    private final Map<String, OpenAiProviderEmbeddingModel> embeddingModels = new HashMap<>();

    public OpenAiProviderModelProvider(OpenAiProviderConfig config, List<String> chatModelIds, List<String> embeddingModelIds) {
        this.providerId = new EntityId("openai");
        this.version = new Version("1.0.0");
        this.config = Objects.requireNonNull(config, "config");
        this.models = new ArrayList<>();

        for (String modelId : chatModelIds) {
            ModelDescriptor descriptor = ModelDescriptor.of(
                new EntityId(modelId), providerId, version, modelId,
                "OpenAI model: " + modelId,
                ModelCapabilities.builder()
                    .chat(true).toolCalling(true).structuredOutput(true)
                    .streaming(true).vision(modelId.contains("gpt-4")).build()
            );
            models.add(descriptor);
            chatModels.put(modelId, new OpenAiProviderChatModel(config, modelId));
        }

        for (String modelId : embeddingModelIds) {
            ModelDescriptor descriptor = ModelDescriptor.of(
                new EntityId(modelId), providerId, version, modelId,
                "OpenAI embedding model: " + modelId,
                ModelCapabilities.embeddingOnly()
            );
            models.add(descriptor);
            embeddingModels.put(modelId, new OpenAiProviderEmbeddingModel(config, modelId));
        }
    }

    public static OpenAiProviderModelProvider standard(OpenAiProviderConfig config) {
        return new OpenAiProviderModelProvider(
            config,
            List.of("gpt-4", "gpt-4-turbo", "gpt-3.5-turbo"),
            List.of("text-embedding-3-small", "text-embedding-3-large")
        );
    }

    public static OpenAiProviderModelProvider of(OpenAiProviderConfig config, String modelId) {
        return new OpenAiProviderModelProvider(config, List.of(modelId), List.of());
    }

    @Override
    public EntityId providerId() { return providerId; }

    @Override
    public Version version() { return version; }

    @Override
    public List<ModelDescriptor> availableModels() { return List.copyOf(models); }

    @Override
    public ChatModel chatModel(EntityId modelId) {
        OpenAiProviderChatModel model = chatModels.get(modelId.value());
        if (model == null) throw new ModelUnavailableException("Model not available: " + modelId, modelId.value(), "openai");
        return model;
    }

    @Override
    public EmbeddingModel embeddingModel(EntityId modelId) {
        OpenAiProviderEmbeddingModel model = embeddingModels.get(modelId.value());
        if (model == null) throw new ModelUnavailableException("Embedding model not available: " + modelId, modelId.value(), "openai");
        return model;
    }

    @Override
    public RerankModel rerankModel(EntityId modelId) {
        throw new ModelUnavailableException("Rerank not supported by OpenAI provider", modelId.value(), "openai");
    }

    @Override
    public StructuredOutputModel structuredOutputModel(EntityId modelId) {
        ChatModel chat = chatModel(modelId);
        return new OpenAiProviderStructuredOutputModel(chat);
    }

    @Override
    public ModelProviderHealth health() {
        return chatModels.isEmpty() ? ModelProviderHealth.UNKNOWN : ModelProviderHealth.AVAILABLE;
    }

    @Override
    public boolean supports(EntityId modelId) {
        return chatModels.containsKey(modelId.value()) || embeddingModels.containsKey(modelId.value());
    }

    @Override
    public ProviderDescriptor descriptor() {
        return ProviderDescriptor.of(
            providerId, ProviderType.REMOTE, version,
            "OpenAI", "OpenAI API Provider",
            availableModels()
        );
    }
}
