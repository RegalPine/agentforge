package io.github.regalpine.agentforge.provider.anthropic;

import io.github.regalpine.agentforge.kernel.EntityId;
import io.github.regalpine.agentforge.kernel.Version;
import io.github.regalpine.agentforge.model.*;

import java.util.*;

/**
 * Anthropic ModelProvider implementation.
 */
public class AnthropicModelProvider implements ModelProvider {

    private final EntityId providerId;
    private final Version version;
    private final AnthropicConfig config;
    private final List<ModelDescriptor> models;
    private final Map<String, AnthropicChatModel> chatModels = new HashMap<>();

    public AnthropicModelProvider(AnthropicConfig config, List<String> modelIds) {
        this.providerId = new EntityId("anthropic");
        this.version = new Version("1.0.0");
        this.config = Objects.requireNonNull(config, "config");
        this.models = new ArrayList<>();

        for (String modelId : modelIds) {
            ModelDescriptor descriptor = ModelDescriptor.of(
                new EntityId(modelId), providerId, version, modelId,
                "Anthropic Claude model: " + modelId,
                ModelCapabilities.builder()
                    .chat(true).toolCalling(true).structuredOutput(true)
                    .streaming(true).vision(modelId.contains("sonnet") || modelId.contains("opus"))
                    .maxContextLength(200000)
                    .build()
            );
            models.add(descriptor);
            chatModels.put(modelId, new AnthropicChatModel(config, modelId));
        }
    }

    public static AnthropicModelProvider standard(AnthropicConfig config) {
        return new AnthropicModelProvider(config, List.of(
            "claude-sonnet-4-20250514", "claude-3-5-sonnet-20241022", "claude-3-haiku-20240307"
        ));
    }

    public static AnthropicModelProvider of(AnthropicConfig config, String modelId) {
        return new AnthropicModelProvider(config, List.of(modelId));
    }

    @Override public EntityId providerId() { return providerId; }
    @Override public Version version() { return version; }
    @Override public List<ModelDescriptor> availableModels() { return List.copyOf(models); }

    @Override
    public ChatModel chatModel(EntityId modelId) {
        AnthropicChatModel model = chatModels.get(modelId.value());
        if (model == null) throw new ModelUnavailableException("Model not available: " + modelId, modelId.value(), "anthropic");
        return model;
    }

    @Override
    public EmbeddingModel embeddingModel(EntityId modelId) {
        throw new ModelUnavailableException("Anthropic does not support embedding models", modelId.value(), "anthropic");
    }

    @Override
    public RerankModel rerankModel(EntityId modelId) {
        throw new ModelUnavailableException("Anthropic does not support rerank models", modelId.value(), "anthropic");
    }

    @Override
    public StructuredOutputModel structuredOutputModel(EntityId modelId) {
        throw new UnsupportedOperationException("Use chat model with tool_use for structured output");
    }

    @Override public ModelProviderHealth health() { return chatModels.isEmpty() ? ModelProviderHealth.UNKNOWN : ModelProviderHealth.AVAILABLE; }
    @Override public boolean supports(EntityId modelId) { return chatModels.containsKey(modelId.value()); }

    @Override
    public ProviderDescriptor descriptor() {
        return ProviderDescriptor.of(providerId, ProviderType.REMOTE, version, "Anthropic", "Anthropic Claude API Provider", availableModels());
    }
}
