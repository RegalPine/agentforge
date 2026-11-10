package io.github.regalpine.agentforge.model;

import io.github.regalpine.agentforge.kernel.EntityId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class InMemoryModelRegistryTest {

    private InMemoryModelRegistry registry;
    private TestModelProvider provider;

    @BeforeEach
    void setUp() {
        registry = new InMemoryModelRegistry();
        provider = new TestModelProvider();
    }

    @Test
    void shouldRegisterProvider() {
        registry.register(provider);

        assertTrue(registry.getProvider(provider.providerId()).isPresent());
    }

    @Test
    void shouldRejectDuplicateProvider() {
        registry.register(provider);

        assertThrows(IllegalStateException.class, () ->
            registry.register(provider)
        );
    }

    @Test
    void shouldUnregisterProvider() {
        registry.register(provider);
        boolean removed = registry.unregister(provider.providerId());

        assertTrue(removed);
        assertFalse(registry.getProvider(provider.providerId()).isPresent());
    }

    @Test
    void shouldReturnFalseForUnregisteringNonExistentProvider() {
        boolean removed = registry.unregister(new EntityId("nonexistent"));

        assertFalse(removed);
    }

    @Test
    void shouldListAllModels() {
        registry.register(provider);

        List<ModelDescriptor> models = registry.listModels();

        assertEquals(1, models.size());
        assertEquals("test-model", models.get(0).modelId().value());
    }

    @Test
    void shouldResolveChatModel() {
        registry.register(provider);

        ModelReference ref = ModelReference.of("test-provider", "test-model");
        ChatModel model = registry.resolveChatModel(ref);

        assertNotNull(model);
        assertEquals("test-model", model.modelId());
    }

    @Test
    void shouldThrowWhenProviderNotFound() {
        ModelReference ref = ModelReference.of("nonexistent", "test-model");

        assertThrows(ModelUnavailableException.class, () ->
            registry.resolveChatModel(ref)
        );
    }

    @Test
    void shouldThrowWhenModelNotSupported() {
        registry.register(provider);

        ModelReference ref = ModelReference.of("test-provider", "nonexistent-model");

        assertThrows(ModelUnavailableException.class, () ->
            registry.resolveChatModel(ref)
        );
    }

    @Test
    void shouldFindModel() {
        registry.register(provider);

        ModelReference ref = ModelReference.of("test-provider", "test-model");
        var descriptor = registry.findModel(ref);

        assertTrue(descriptor.isPresent());
        assertEquals("test-model", descriptor.get().modelId().value());
    }

    @Test
    void shouldReturnEmptyForUnknownModel() {
        ModelReference ref = ModelReference.of("nonexistent", "unknown");
        var descriptor = registry.findModel(ref);

        assertFalse(descriptor.isPresent());
    }

    /**
     * Test model provider for unit tests.
     */
    private static class TestModelProvider implements ModelProvider {

        @Override
        public EntityId providerId() {
            return new EntityId("test-provider");
        }

        @Override
        public io.github.regalpine.agentforge.kernel.Version version() {
            return new io.github.regalpine.agentforge.kernel.Version("1.0.0");
        }

        @Override
        public List<ModelDescriptor> availableModels() {
            return List.of(
                ModelDescriptor.of(
                    new EntityId("test-model"),
                    providerId(),
                    version(),
                    "Test Model",
                    "A test model",
                    ModelCapabilities.all()
                )
            );
        }

        @Override
        public ChatModel chatModel(EntityId modelId) {
            if (!"test-model".equals(modelId.value())) {
                throw new ModelUnavailableException("Model not found: " + modelId);
            }
            return new TestChatModel();
        }

        @Override
        public EmbeddingModel embeddingModel(EntityId modelId) {
            throw new ModelUnavailableException("Embedding not supported");
        }

        @Override
        public RerankModel rerankModel(EntityId modelId) {
            throw new ModelUnavailableException("Rerank not supported");
        }

        @Override
        public StructuredOutputModel structuredOutputModel(EntityId modelId) {
            throw new ModelUnavailableException("Structured output not supported");
        }

        @Override
        public ModelProviderHealth health() {
            return ModelProviderHealth.AVAILABLE;
        }

        @Override
        public boolean supports(EntityId modelId) {
            return "test-model".equals(modelId.value());
        }
    }

    /**
     * Test chat model for unit tests.
     */
    private static class TestChatModel implements ChatModel {

        @Override
        public ChatResponse chat(ChatRequest request) {
            return ChatResponse.of("Test response");
        }

        @Override
        public String modelId() {
            return "test-model";
        }

        @Override
        public String providerId() {
            return "test-provider";
        }
    }
}
