package io.github.regalpine.agentforge.provider.llm.openai;

import io.github.regalpine.agentforge.kernel.EntityId;
import io.github.regalpine.agentforge.model.*;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class OpenAiModelProviderTest {

    @Test
    void shouldCreateProviderWithSingleModel() {
        OpenAiConfig config = OpenAiConfig.openAi("sk-test");
        OpenAiModelProvider provider = OpenAiModelProvider.of(config, "gpt-4");

        assertEquals("openai", provider.providerId().value());
        assertEquals("1.0.0", provider.version().value());
        assertTrue(provider.supports(new EntityId("gpt-4")));
        assertFalse(provider.supports(new EntityId("gpt-3.5")));
        assertEquals(1, provider.availableModels().size());
    }

    @Test
    void shouldCreateProviderWithMultipleModels() {
        OpenAiConfig config = OpenAiConfig.openAi("sk-test");
        OpenAiModelProvider provider = OpenAiModelProvider.standard(config);

        assertTrue(provider.supports(new EntityId("gpt-4")));
        assertTrue(provider.supports(new EntityId("gpt-4-turbo")));
        assertTrue(provider.supports(new EntityId("gpt-3.5-turbo")));
        assertEquals(3, provider.availableModels().size());
    }

    @Test
    void shouldResolveChatModel() {
        OpenAiConfig config = OpenAiConfig.openAi("sk-test");
        OpenAiModelProvider provider = OpenAiModelProvider.of(config, "gpt-4");

        ChatModel model = provider.chatModel(new EntityId("gpt-4"));
        assertNotNull(model);
        assertEquals("gpt-4", model.modelId());
        assertEquals("openai", model.providerId());
    }

    @Test
    void shouldThrowForUnsupportedModel() {
        OpenAiConfig config = OpenAiConfig.openAi("sk-test");
        OpenAiModelProvider provider = OpenAiModelProvider.of(config, "gpt-4");

        assertThrows(ModelUnavailableException.class, () ->
            provider.chatModel(new EntityId("nonexistent"))
        );
    }

    @Test
    void shouldThrowForEmbeddingModel() {
        OpenAiConfig config = OpenAiConfig.openAi("sk-test");
        OpenAiModelProvider provider = OpenAiModelProvider.of(config, "gpt-4");

        assertThrows(ModelUnavailableException.class, () ->
            provider.embeddingModel(new EntityId("gpt-4"))
        );
    }

    @Test
    void shouldThrowForRerankModel() {
        OpenAiConfig config = OpenAiConfig.openAi("sk-test");
        OpenAiModelProvider provider = OpenAiModelProvider.of(config, "gpt-4");

        assertThrows(ModelUnavailableException.class, () ->
            provider.rerankModel(new EntityId("gpt-4"))
        );
    }

    @Test
    void shouldResolveStructuredOutputModel() {
        OpenAiConfig config = OpenAiConfig.openAi("sk-test");
        OpenAiModelProvider provider = OpenAiModelProvider.of(config, "gpt-4");

        StructuredOutputModel model = provider.structuredOutputModel(new EntityId("gpt-4"));
        assertNotNull(model);
        assertEquals("gpt-4", model.modelId());
    }

    @Test
    void shouldReportHealth() {
        OpenAiConfig config = OpenAiConfig.openAi("sk-test");
        OpenAiModelProvider provider = OpenAiModelProvider.of(config, "gpt-4");

        assertEquals(ModelProviderHealth.AVAILABLE, provider.health());
    }

    @Test
    void shouldListModelDescriptors() {
        OpenAiConfig config = OpenAiConfig.openAi("sk-test");
        OpenAiModelProvider provider = OpenAiModelProvider.of(config, "gpt-4");

        List<ModelDescriptor> models = provider.availableModels();
        assertEquals(1, models.size());

        ModelDescriptor desc = models.get(0);
        assertEquals("gpt-4", desc.modelId().value());
        assertEquals("openai", desc.providerId().value());
        assertTrue(desc.capabilities().chat());
        assertTrue(desc.capabilities().toolCalling());
        assertTrue(desc.capabilities().structuredOutput());
    }
}
