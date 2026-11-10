package io.github.regalpine.agentforge.provider.openai;

import io.github.regalpine.agentforge.model.*;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Edge-case tests for OpenAiJson.
 * Covers: Unicode, nested JSON, streaming, tool calls, empty/minimal responses.
 */
class OpenAiJsonEdgeCaseTest {

    // ── Chat response parsing ──

    @Test
    void shouldParseMinimalChatResponse() {
        String json = """
            {
              "id": "chatcmpl-123",
              "model": "gpt-4",
              "choices": [{
                "index": 0,
                "message": {"role": "assistant", "content": "Hello"},
                "finish_reason": "stop"
              }]
            }
            """;

        ChatResponse resp = OpenAiJson.parseChatResponse(json);
        assertNotNull(resp);
        assertEquals("Hello", resp.content());
        assertEquals("gpt-4", resp.model());
    }

    @Test
    void shouldParseResponseWithUnicodeContent() {
        String json = """
            {
              "id": "unicode-1",
              "model": "gpt-4",
              "choices": [{
                "index": 0,
                "message": {"role": "assistant", "content": "你好世界 🌍 café"},
                "finish_reason": "stop"
              }]
            }
            """;

        ChatResponse resp = OpenAiJson.parseChatResponse(json);
        assertEquals("你好世界 🌍 café", resp.content());
    }

    @Test
    void shouldParseResponseWithNestedJsonInContent() {
        String json = """
            {
              "id": "nested-1",
              "model": "gpt-4",
              "choices": [{
                "index": 0,
                "message": {"role": "assistant", "content": "{\\"key\\": \\"value with \\\\\\"quotes\\\\\\"\\"}"},
                "finish_reason": "stop"
              }]
            }
            """;

        ChatResponse resp = OpenAiJson.parseChatResponse(json);
        String content = resp.content();
        assertTrue(content.contains("key"));
        assertTrue(content.contains("value"));
    }

    @Test
    void shouldParseResponseWithUsage() {
        String json = """
            {
              "id": "usage-1",
              "model": "gpt-4",
              "choices": [{
                "index": 0,
                "message": {"role": "assistant", "content": "Hi"},
                "finish_reason": "stop"
              }],
              "usage": {"prompt_tokens": 10, "completion_tokens": 5, "total_tokens": 15}
            }
            """;

        ChatResponse resp = OpenAiJson.parseChatResponse(json);
        assertNotNull(resp.usage());
        assertEquals(10, resp.usage().inputTokens());
        assertEquals(5, resp.usage().outputTokens());
        assertEquals(15, resp.usage().totalTokens());
    }

    @Test
    void shouldParseResponseWithEmptyContent() {
        String json = """
            {
              "id": "empty-1",
              "model": "gpt-4",
              "choices": [{
                "index": 0,
                "message": {"role": "assistant", "content": ""},
                "finish_reason": "stop"
              }]
            }
            """;

        ChatResponse resp = OpenAiJson.parseChatResponse(json);
        assertEquals("", resp.content());
    }

    // ── Stream event parsing ──

    @Test
    void shouldParseStreamEventWithContent() {
        String data = """
            {"id":"stream-1","choices":[{"delta":{"content":"Hello"},"index":0}]}
            """;

        ChatEvent event = OpenAiJson.parseStreamEvent(data.trim());
        assertNotNull(event);
    }

    @Test
    void shouldHandleStreamDoneSentinel() {
        ChatEvent event = OpenAiJson.parseStreamEvent("[DONE]");
        assertNotNull(event);
    }

    @Test
    void shouldParseStreamEventWithFinishReason() {
        String data = """
            {"id":"stream-2","choices":[{"delta":{},"index":0,"finish_reason":"stop"}]}
            """;

        ChatEvent event = OpenAiJson.parseStreamEvent(data.trim());
        assertNotNull(event);
    }

    // ── Request body building ──

    @Test
    void shouldBuildRequestBodyWithBasicMessage() {
        ChatRequest request = ChatRequest.of(
            List.of(new UserMessage("Hello"))
        );

        String body = OpenAiJson.buildChatRequestBody("gpt-4", request, false);
        assertNotNull(body);
        assertTrue(body.contains("\"model\":\"gpt-4\""));
        assertTrue(body.contains("\"content\":\"Hello\""));
        assertFalse(body.contains("\"stream\""));
    }

    @Test
    void shouldBuildRequestBodyWithStreamFlag() {
        ChatRequest request = ChatRequest.of(
            List.of(new UserMessage("Hi"))
        );

        String body = OpenAiJson.buildChatRequestBody("gpt-4", request, true);
        assertTrue(body.contains("\"stream\":true"));
    }

    @Test
    void shouldEscapeSpecialCharactersInContent() {
        ChatRequest request = ChatRequest.of(
            List.of(new UserMessage("Line1\nLine2\t\"quoted\""))
        );

        String body = OpenAiJson.buildChatRequestBody("gpt-4", request, false);
        assertNotNull(body);
        // Jackson handles escaping - verify the JSON is valid
        assertTrue(body.contains("Line1"));
    }

    @Test
    void shouldBuildRequestBodyWithTemperature() {
        ChatRequest request = new ChatRequest(
            List.of(new UserMessage("Test")),
            null, 0.7, 100, null, null, null, null
        );

        String body = OpenAiJson.buildChatRequestBody("gpt-4", request, false);
        assertTrue(body.contains("\"temperature\":0.7"));
        assertTrue(body.contains("\"max_tokens\":100"));
    }

    // ── JSON map parsing ──

    @Test
    void shouldParseSimpleJsonMap() {
        String json = "{\"key\":\"value\",\"num\":42}";
        Map<String, Object> map = OpenAiJson.parseJsonMap(json);

        assertEquals("value", map.get("key"));
        // G3-6: small integers are now Integer, not Long
        assertEquals(42, map.get("num"));
    }

    @Test
    void shouldParseNestedJsonMap() {
        String json = "{\"outer\":{\"inner\":\"deep\"}}";
        Map<String, Object> map = OpenAiJson.parseJsonMap(json);

        // G3-6: nested objects are now Map, not raw JSON string
        assertNotNull(map.get("outer"));
        assertInstanceOf(Map.class, map.get("outer"));
        @SuppressWarnings("unchecked")
        Map<String, Object> inner = (Map<String, Object>) map.get("outer");
        assertEquals("deep", inner.get("inner"));
    }

    @Test
    void shouldParseEmptyJsonMap() {
        String json = "{}";
        Map<String, Object> map = OpenAiJson.parseJsonMap(json);
        assertTrue(map.isEmpty());
    }

    // ── Map to JSON conversion ──

    @Test
    void shouldConvertMapToJson() {
        Map<String, Object> map = Map.of("a", "b", "c", 1);
        String json = OpenAiJson.mapToJson(map);

        assertNotNull(json);
        assertTrue(json.contains("\"a\""));
        assertTrue(json.contains("\"b\""));
    }

    @Test
    void shouldHandleUnicodeInMapToJson() {
        Map<String, Object> map = Map.of("greeting", "你好");
        String json = OpenAiJson.mapToJson(map);

        assertNotNull(json);
        assertTrue(json.contains("你好") || json.contains("\\u"));
    }
}
