package io.github.regalpine.agentforge.provider.llm.openai;

import io.github.regalpine.agentforge.kernel.EntityId;
import io.github.regalpine.agentforge.model.*;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class OpenAiConfigTest {

    @Test
    void shouldCreateOpenAiConfig() {
        OpenAiConfig config = OpenAiConfig.openAi("sk-test-key");

        assertEquals("https://api.openai.com/v1", config.baseUrl().toString());
        assertEquals("sk-test-key", config.apiKey());
        assertNull(config.organizationId());
        assertEquals(Duration.ofSeconds(10), config.connectTimeout());
        assertEquals(Duration.ofSeconds(60), config.readTimeout());
        assertEquals("gpt-4", config.defaultModel());
    }

    @Test
    void shouldCreateCompatibleConfig() {
        OpenAiConfig config = OpenAiConfig.compatible("http://localhost:11434/v1", "ollama");

        assertEquals("http://localhost:11434/v1", config.baseUrl().toString());
        assertEquals("ollama", config.apiKey());
        assertEquals("default", config.defaultModel());
    }

    @Test
    void shouldBuildCustomConfig() {
        OpenAiConfig config = OpenAiConfig.builder()
            .baseUrl("http://custom:8080/v1")
            .apiKey("my-key")
            .organizationId("org-123")
            .connectTimeout(Duration.ofSeconds(5))
            .readTimeout(Duration.ofSeconds(30))
            .defaultModel("custom-model")
            .build();

        assertEquals("http://custom:8080/v1", config.baseUrl().toString());
        assertEquals("my-key", config.apiKey());
        assertEquals("org-123", config.organizationId());
        assertEquals(Duration.ofSeconds(5), config.connectTimeout());
        assertEquals(Duration.ofSeconds(30), config.readTimeout());
        assertEquals("custom-model", config.defaultModel());
    }

    @Test
    void shouldRejectBlankApiKey() {
        assertThrows(IllegalArgumentException.class, () ->
            OpenAiConfig.openAi("")
        );
        assertThrows(NullPointerException.class, () ->
            OpenAiConfig.openAi(null)
        );
    }
}
