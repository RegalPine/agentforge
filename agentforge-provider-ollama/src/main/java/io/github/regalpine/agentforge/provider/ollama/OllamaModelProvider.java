package io.github.regalpine.agentforge.provider.ollama;

import io.github.regalpine.agentforge.kernel.EntityId;
import io.github.regalpine.agentforge.kernel.Version;
import io.github.regalpine.agentforge.model.*;

import java.util.*;

/**
 * Ollama ModelProvider implementation.
 * <p>
 * Supports local and remote Ollama instances with Chat, Embedding, and Streaming.
 */
public class OllamaModelProvider implements ModelProvider {

    private final EntityId providerId;
    private final Version version;
    private final OllamaConfig config;
    private final List<ModelDescriptor> models;
    private final Map<String, OllamaChatModel> chatModels = new HashMap<>();
    private final Map<String, OllamaEmbeddingModel> embeddingModels = new HashMap<>();

    public OllamaModelProvider(OllamaConfig config, List<String> chatModelIds, List<String> embeddingModelIds) {
        this.providerId = new EntityId("ollama");
        this.version = new Version("1.0.0");
        this.config = Objects.requireNonNull(config, "config");
        this.models = new ArrayList<>();

        for (String modelId : chatModelIds) {
            ModelDescriptor descriptor = ModelDescriptor.of(
                new EntityId(modelId), providerId, version, modelId,
                "Ollama model: " + modelId,
                ModelCapabilities.builder()
                    .chat(true).toolCalling(true).streaming(true).build()
            );
            models.add(descriptor);
            chatModels.put(modelId, new OllamaChatModel(config, modelId));
        }

        for (String modelId : embeddingModelIds) {
            ModelDescriptor descriptor = ModelDescriptor.of(
                new EntityId(modelId), providerId, version, modelId,
                "Ollama embedding model: " + modelId,
                ModelCapabilities.embeddingOnly()
            );
            models.add(descriptor);
            embeddingModels.put(modelId, new OllamaEmbeddingModel(config, modelId));
        }
    }

    public static OllamaModelProvider local(OllamaConfig config) {
        return new OllamaModelProvider(config, List.of("llama3"), List.of("nomic-embed-text"));
    }

    public static OllamaModelProvider of(OllamaConfig config, String modelId) {
        return new OllamaModelProvider(config, List.of(modelId), List.of());
    }

    @Override public EntityId providerId() { return providerId; }
    @Override public Version version() { return version; }
    @Override public List<ModelDescriptor> availableModels() { return List.copyOf(models); }

    @Override
    public ChatModel chatModel(EntityId modelId) {
        OllamaChatModel model = chatModels.get(modelId.value());
        if (model == null) throw new ModelUnavailableException("Model not available: " + modelId, modelId.value(), "ollama");
        return model;
    }

    @Override
    public EmbeddingModel embeddingModel(EntityId modelId) {
        OllamaEmbeddingModel model = embeddingModels.get(modelId.value());
        if (model == null) throw new ModelUnavailableException("Embedding model not available: " + modelId, modelId.value(), "ollama");
        return model;
    }

    @Override
    public RerankModel rerankModel(EntityId modelId) {
        throw new ModelUnavailableException("Rerank not supported by Ollama provider", modelId.value(), "ollama");
    }

    @Override
    public StructuredOutputModel structuredOutputModel(EntityId modelId) {
        throw new UnsupportedOperationException("Ollama structured output not yet implemented");
    }

    @Override public ModelProviderHealth health() { return chatModels.isEmpty() ? ModelProviderHealth.UNKNOWN : ModelProviderHealth.AVAILABLE; }
    @Override public boolean supports(EntityId modelId) { return chatModels.containsKey(modelId.value()) || embeddingModels.containsKey(modelId.value()); }

    @Override
    public ProviderDescriptor descriptor() {
        return ProviderDescriptor.of(providerId, ProviderType.LOCAL, version, "Ollama", "Ollama Local/Remote Provider", availableModels());
    }
}
