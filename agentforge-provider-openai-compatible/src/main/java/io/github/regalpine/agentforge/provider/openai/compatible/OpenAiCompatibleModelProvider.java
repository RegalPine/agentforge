package io.github.regalpine.agentforge.provider.openai.compatible;

import io.github.regalpine.agentforge.kernel.EntityId;
import io.github.regalpine.agentforge.kernel.Version;
import io.github.regalpine.agentforge.model.*;

import java.util.*;

/**
 * OpenAI-Compatible ModelProvider implementation.
 * <p>
 * A single implementation that works with vLLM, LM Studio, enterprise
 * model gateways, and any OpenAI API-compatible service.
 * <p>
 * This is the recommended Provider for connecting to internal model services
 * rather than creating separate provider modules for each compatible service.
 */
public class OpenAiCompatibleModelProvider implements ModelProvider {

    private final EntityId providerId;
    private final Version version;
    private final OpenAiCompatibleConfig config;
    private final List<ModelDescriptor> models;
    private final Map<String, OpenAiCompatibleChatModel> chatModels = new HashMap<>();

    public OpenAiCompatibleModelProvider(OpenAiCompatibleConfig config, List<String> modelIds) {
        this.providerId = new EntityId(config.providerName());
        this.version = new Version("1.0.0");
        this.config = Objects.requireNonNull(config, "config");
        this.models = new ArrayList<>();

        for (String modelId : modelIds) {
            ModelDescriptor descriptor = ModelDescriptor.of(
                new EntityId(modelId), providerId, version, modelId,
                config.providerName() + " model: " + modelId,
                ModelCapabilities.builder()
                    .chat(true).toolCalling(true).structuredOutput(true)
                    .streaming(true).build()
            );
            models.add(descriptor);
            chatModels.put(modelId, new OpenAiCompatibleChatModel(config, modelId));
        }
    }

    /**
     * Create a provider for a vLLM server.
     *
     * @param baseUrl  the vLLM server URL
     * @param modelIds the available model IDs
     * @return a new provider
     */
    public static OpenAiCompatibleModelProvider vllm(String baseUrl, List<String> modelIds) {
        return new OpenAiCompatibleModelProvider(OpenAiCompatibleConfig.vllm(baseUrl), modelIds);
    }

    /**
     * Create a provider for LM Studio.
     *
     * @param modelIds the available model IDs
     * @return a new provider
     */
    public static OpenAiCompatibleModelProvider lmStudio(List<String> modelIds) {
        return new OpenAiCompatibleModelProvider(OpenAiCompatibleConfig.lmStudio(), modelIds);
    }

    /**
     * Create a provider for an enterprise gateway.
     *
     * @param baseUrl  the gateway URL
     * @param apiKey   the API key
     * @param modelIds the available model IDs
     * @return a new provider
     */
    public static OpenAiCompatibleModelProvider gateway(String baseUrl, String apiKey, List<String> modelIds) {
        return new OpenAiCompatibleModelProvider(OpenAiCompatibleConfig.gateway(baseUrl, apiKey), modelIds);
    }

    public static OpenAiCompatibleModelProvider of(OpenAiCompatibleConfig config, String modelId) {
        return new OpenAiCompatibleModelProvider(config, List.of(modelId));
    }

    @Override public EntityId providerId() { return providerId; }
    @Override public Version version() { return version; }
    @Override public List<ModelDescriptor> availableModels() { return List.copyOf(models); }

    @Override
    public ChatModel chatModel(EntityId modelId) {
        OpenAiCompatibleChatModel model = chatModels.get(modelId.value());
        if (model == null) throw new ModelUnavailableException("Model not available: " + modelId, modelId.value(), config.providerName());
        return model;
    }

    @Override
    public EmbeddingModel embeddingModel(EntityId modelId) {
        throw new ModelUnavailableException("Embedding not supported by compatible provider", modelId.value(), config.providerName());
    }

    @Override
    public RerankModel rerankModel(EntityId modelId) {
        throw new ModelUnavailableException("Rerank not supported by compatible provider", modelId.value(), config.providerName());
    }

    @Override
    public StructuredOutputModel structuredOutputModel(EntityId modelId) {
        throw new UnsupportedOperationException("Use chat model with response_format for structured output");
    }

    @Override public ModelProviderHealth health() { return chatModels.isEmpty() ? ModelProviderHealth.UNKNOWN : ModelProviderHealth.AVAILABLE; }
    @Override public boolean supports(EntityId modelId) { return chatModels.containsKey(modelId.value()); }

    @Override
    public ProviderDescriptor descriptor() {
        return ProviderDescriptor.of(
            providerId, ProviderType.OPENAI_COMPATIBLE, version,
            config.providerName(), "OpenAI-Compatible Provider: " + config.baseUrl(),
            availableModels()
        );
    }
}
