package io.github.regalpine.agentforge.model;

import io.github.regalpine.agentforge.kernel.EntityId;
import io.github.regalpine.agentforge.kernel.Version;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Currency;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for the Provider Ecosystem enhancements (H0).
 */
class ProviderEcosystemTest {

    // --- ChatEvent sealed interface ---

    @Test
    void chatEvent_streamStarted() {
        ChatEvent event = new ChatEvent.ChatStreamStarted("gpt-4", "openai");
        assertInstanceOf(ChatEvent.ChatStreamStarted.class, event);
        assertEquals("gpt-4", ((ChatEvent.ChatStreamStarted) event).model());
        assertEquals("openai", ((ChatEvent.ChatStreamStarted) event).providerId());
    }

    @Test
    void chatEvent_textDelta() {
        ChatEvent event = new ChatEvent.TextDelta("Hello");
        assertInstanceOf(ChatEvent.TextDelta.class, event);
        assertEquals("Hello", ((ChatEvent.TextDelta) event).delta());
    }

    @Test
    void chatEvent_toolCallStarted() {
        ChatEvent event = new ChatEvent.ToolCallStarted("call-1", "search");
        assertInstanceOf(ChatEvent.ToolCallStarted.class, event);
    }

    @Test
    void chatEvent_toolCallDelta() {
        ChatEvent event = new ChatEvent.ToolCallDelta("call-1", "{\"query\":");
        assertInstanceOf(ChatEvent.ToolCallDelta.class, event);
    }

    @Test
    void chatEvent_toolCallCompleted() {
        ChatEvent event = new ChatEvent.ToolCallCompleted("call-1", "search", "{\"query\":\"test\"}");
        assertInstanceOf(ChatEvent.ToolCallCompleted.class, event);
    }

    @Test
    void chatEvent_usageReceived() {
        ChatEvent event = new ChatEvent.UsageReceived(TokenUsage.of(10, 20));
        assertInstanceOf(ChatEvent.UsageReceived.class, event);
    }

    @Test
    void chatEvent_completed() {
        ChatEvent event = new ChatEvent.ChatStreamCompleted(FinishReason.STOP);
        assertInstanceOf(ChatEvent.ChatStreamCompleted.class, event);
    }

    @Test
    void chatEvent_failed() {
        ChatEvent event = new ChatEvent.ChatStreamFailed("error");
        assertInstanceOf(ChatEvent.ChatStreamFailed.class, event);
        assertEquals("error", ((ChatEvent.ChatStreamFailed) event).error());
        assertNull(((ChatEvent.ChatStreamFailed) event).cause());
    }

    @Test
    void chatEvent_failedWithCause() {
        Exception cause = new RuntimeException("test");
        ChatEvent event = new ChatEvent.ChatStreamFailed("error", cause);
        assertEquals(cause, ((ChatEvent.ChatStreamFailed) event).cause());
    }

    // --- Credential ---

    @Test
    void credentialReference_apiKey() {
        CredentialReference ref = CredentialReference.apiKey("my-key");
        assertEquals("my-key", ref.credentialId());
        assertEquals("api-key", ref.type());
    }

    @Test
    void credential_apiKey() {
        Credential cred = Credential.apiKey("my-key", "sk-12345");
        assertEquals("my-key", cred.credentialId());
        assertEquals("api-key", cred.type());
        assertEquals("sk-12345", cred.secret());
        assertFalse(cred.isExpired());
    }

    @Test
    void credential_bearerToken() {
        Credential cred = Credential.bearerToken("token-1", "Bearer xyz");
        assertEquals("bearer-token", cred.type());
        assertEquals("Bearer xyz", cred.secret());
    }

    // --- DefaultCredentialProvider ---

    @Test
    void defaultCredentialProvider_registerAndResolve() {
        DefaultCredentialProvider provider = new DefaultCredentialProvider();
        provider.registerApiKey("test-key", "sk-test");

        CredentialReference ref = CredentialReference.apiKey("test-key");
        assertTrue(provider.supports(ref));
        Credential cred = provider.resolve(ref);
        assertNotNull(cred);
        assertEquals("sk-test", cred.secret());
    }

    @Test
    void defaultCredentialProvider_unsupportedThrows() {
        DefaultCredentialProvider provider = new DefaultCredentialProvider();
        CredentialReference ref = CredentialReference.apiKey("unknown");
        assertThrows(ModelException.class, () -> provider.resolve(ref));
    }

    // --- ModelUsage ---

    @Test
    void modelUsage_basic() {
        ModelUsage usage = ModelUsage.of(100, 50);
        assertEquals(100, usage.inputTokens());
        assertEquals(50, usage.outputTokens());
        assertEquals(150, usage.totalTokens());
        assertEquals(0, usage.cachedInputTokens());
        assertEquals(0, usage.reasoningTokens());
    }

    @Test
    void modelUsage_withCachingAndReasoning() {
        ModelUsage usage = new ModelUsage(100, 50, 150, 30, 20, Map.of());
        assertEquals(30, usage.cachedInputTokens());
        assertEquals(20, usage.reasoningTokens());
    }

    @Test
    void modelUsage_add() {
        ModelUsage a = ModelUsage.of(100, 50);
        ModelUsage b = ModelUsage.of(200, 100);
        ModelUsage sum = a.add(b);
        assertEquals(300, sum.inputTokens());
        assertEquals(150, sum.outputTokens());
    }

    @Test
    void modelUsage_toTokenUsage() {
        ModelUsage usage = ModelUsage.of(100, 50);
        TokenUsage tokenUsage = usage.toTokenUsage();
        assertEquals(100, tokenUsage.inputTokens());
        assertEquals(50, tokenUsage.outputTokens());
    }

    // --- ModelPricing & ModelCost ---

    @Test
    void modelPricing_calculate() {
        ModelPricing pricing = ModelPricing.perMillionTokens("gpt-4", "openai", new BigDecimal("30"), new BigDecimal("60"), Currency.getInstance("USD"));
        ModelUsage usage = ModelUsage.of(1000, 500);
        ModelCost cost = pricing.calculate(usage);
        assertNotNull(cost.inputCost());
        assertNotNull(cost.outputCost());
        assertNotNull(cost.totalCost());
        assertTrue(cost.totalCost().compareTo(BigDecimal.ZERO) > 0);
    }

    @Test
    void modelPricing_free() {
        ModelPricing pricing = ModelPricing.free("local-model", "ollama");
        ModelUsage usage = ModelUsage.of(1000, 500);
        ModelCost cost = pricing.calculate(usage);
        assertEquals(0, cost.totalCost().compareTo(BigDecimal.ZERO));
    }

    @Test
    void modelCost_add() {
        ModelCost a = ModelCost.of(new BigDecimal("1.00"), new BigDecimal("2.00"), Currency.getInstance("USD"), "gpt-4");
        ModelCost b = ModelCost.of(new BigDecimal("0.50"), new BigDecimal("1.00"), Currency.getInstance("USD"), "gpt-4");
        ModelCost sum = a.add(b);
        assertEquals(new BigDecimal("1.50"), sum.inputCost());
        assertEquals(new BigDecimal("3.00"), sum.outputCost());
        assertEquals(new BigDecimal("4.50"), sum.totalCost());
    }

    @Test
    void modelCost_zero() {
        ModelCost zero = ModelCost.zero();
        assertEquals(BigDecimal.ZERO, zero.totalCost());
    }

    // --- DefaultPricingResolver ---

    @Test
    void defaultPricingResolver_registerAndResolve() {
        DefaultPricingResolver resolver = new DefaultPricingResolver();
        ModelPricing pricing = ModelPricing.perMillionTokens("gpt-4", "openai", new BigDecimal("30"), new BigDecimal("60"), Currency.getInstance("USD"));
        resolver.register(pricing);

        ModelPricing resolved = resolver.pricing("openai", "gpt-4");
        assertNotNull(resolved);
        assertEquals("gpt-4", resolved.modelId());
    }

    @Test
    void defaultPricingResolver_unknownReturnsFree() {
        DefaultPricingResolver resolver = new DefaultPricingResolver();
        ModelPricing resolved = resolver.pricing("unknown", "unknown");
        assertNull(resolved);
    }

    // --- ProviderDescriptor & ProviderType ---

    @Test
    void providerDescriptor_of() {
        EntityId providerId = new EntityId("openai");
        Version version = new Version("1.0.0");
        ModelDescriptor model = ModelDescriptor.of(
            new EntityId("gpt-4"), providerId, version, "gpt-4",
            "GPT-4", ModelCapabilities.chatOnly()
        );

        ProviderDescriptor descriptor = ProviderDescriptor.of(
            providerId, ProviderType.REMOTE, version, "OpenAI", "OpenAI API", List.of(model)
        );

        assertEquals("openai", descriptor.providerId().value());
        assertEquals(ProviderType.REMOTE, descriptor.providerType());
        assertEquals(1, descriptor.supportedModels().size());
    }

    @Test
    void providerType_values() {
        assertEquals(4, ProviderType.values().length);
        assertNotNull(ProviderType.REMOTE);
        assertNotNull(ProviderType.OPENAI_COMPATIBLE);
        assertNotNull(ProviderType.LOCAL);
        assertNotNull(ProviderType.GATEWAY);
    }

    // --- ProviderException ---

    @Test
    void providerException_fields() {
        ProviderException ex = new ProviderException("test error", "openai", "REMOTE");
        assertEquals("openai", ex.providerId());
        assertEquals("REMOTE", ex.providerType());
    }

    // --- ChatRequest with providerOptions ---

    @Test
    void chatRequest_providerOptions() {
        ChatRequest request = ChatRequest.builder()
            .messages(List.of(UserMessage.of("hello")))
            .providerOptions(Map.of("top_p", 0.9, "seed", 42))
            .build();

        assertEquals(2, request.providerOptions().size());
        assertEquals(0.9, request.providerOptions().get("top_p"));
        assertEquals(42, request.providerOptions().get("seed"));
    }

    @Test
    void chatRequest_defaultProviderOptionsEmpty() {
        ChatRequest request = ChatRequest.of(List.of(UserMessage.of("hello")));
        assertTrue(request.providerOptions().isEmpty());
    }

    // --- InMemoryModelRegistry enhancements ---

    @Test
    void modelRegistry_healthSummary() {
        InMemoryModelRegistry registry = new InMemoryModelRegistry();

        // Create a mock provider
        ModelProvider mockProvider = new ModelProvider() {
            @Override public EntityId providerId() { return new EntityId("mock"); }
            @Override public Version version() { return new Version("1.0.0"); }
            @Override public List<ModelDescriptor> availableModels() { return List.of(); }
            @Override public ChatModel chatModel(EntityId modelId) { throw new UnsupportedOperationException(); }
            @Override public EmbeddingModel embeddingModel(EntityId modelId) { throw new UnsupportedOperationException(); }
            @Override public RerankModel rerankModel(EntityId modelId) { throw new UnsupportedOperationException(); }
            @Override public StructuredOutputModel structuredOutputModel(EntityId modelId) { throw new UnsupportedOperationException(); }
            @Override public ModelProviderHealth health() { return ModelProviderHealth.AVAILABLE; }
            @Override public boolean supports(EntityId modelId) { return false; }
        };

        registry.register(mockProvider);
        Map<String, ModelProviderHealth> summary = registry.healthSummary();
        assertEquals(1, summary.size());
        assertEquals(ModelProviderHealth.AVAILABLE, summary.get("mock"));
    }

    @Test
    void modelRegistry_listProviderDescriptors() {
        InMemoryModelRegistry registry = new InMemoryModelRegistry();

        ModelProvider mockProvider = new ModelProvider() {
            @Override public EntityId providerId() { return new EntityId("mock"); }
            @Override public Version version() { return new Version("1.0.0"); }
            @Override public List<ModelDescriptor> availableModels() { return List.of(); }
            @Override public ChatModel chatModel(EntityId modelId) { throw new UnsupportedOperationException(); }
            @Override public EmbeddingModel embeddingModel(EntityId modelId) { throw new UnsupportedOperationException(); }
            @Override public RerankModel rerankModel(EntityId modelId) { throw new UnsupportedOperationException(); }
            @Override public StructuredOutputModel structuredOutputModel(EntityId modelId) { throw new UnsupportedOperationException(); }
            @Override public ModelProviderHealth health() { return ModelProviderHealth.AVAILABLE; }
            @Override public boolean supports(EntityId modelId) { return false; }
            @Override public ProviderDescriptor descriptor() {
                return ProviderDescriptor.of(new EntityId("mock"), ProviderType.REMOTE, new Version("1.0.0"), "Mock", "Mock Provider", List.of());
            }
        };

        registry.register(mockProvider);
        List<ProviderDescriptor> descriptors = registry.listProviderDescriptors();
        assertEquals(1, descriptors.size());
        assertEquals("mock", descriptors.get(0).providerId().value());
        assertEquals(ProviderType.REMOTE, descriptors.get(0).providerType());
    }
}
