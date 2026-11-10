package io.github.regalpine.agentforge.provider.llm.openai;

import com.fasterxml.jackson.core.*;
import io.github.regalpine.agentforge.model.*;

import java.io.IOException;
import java.io.StringWriter;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.*;

/**
 * OpenAI-compatible ChatModel implementation using JDK HttpClient.
 * <p>
 * Uses {@code jackson-core} for robust JSON handling.
 * <p>
 * Supports:
 * <ul>
 *   <li>OpenAI Chat Completions API</li>
 *   <li>Any OpenAI-compatible endpoint (vLLM, Ollama, etc.)</li>
 *   <li>Tool calling</li>
 *   <li>Response format specification</li>
 * </ul>
 */
public class OpenAiChatModel implements ChatModel {

    private static final JsonFactory JSON_FACTORY = new JsonFactory();

    private final OpenAiConfig config;
    private final HttpClient httpClient;
    private final String modelId;

    public OpenAiChatModel(OpenAiConfig config, String modelId) {
        this.config = Objects.requireNonNull(config, "config");
        this.modelId = Objects.requireNonNull(modelId, "modelId");
        this.httpClient = HttpClient.newBuilder()
            .connectTimeout(config.connectTimeout())
            .build();
    }

    public OpenAiChatModel(OpenAiConfig config) {
        this(config, config.defaultModel());
    }

    @Override
    public ChatResponse chat(ChatRequest request) {
        Objects.requireNonNull(request, "request");

        String requestBody = buildRequestBody(request);

        HttpRequest.Builder httpRequestBuilder = HttpRequest.newBuilder()
            .uri(URI.create(config.baseUrl() + "/chat/completions"))
            .header("Content-Type", "application/json")
            .header("Authorization", "Bearer " + config.apiKey())
            .timeout(config.readTimeout())
            .POST(HttpRequest.BodyPublishers.ofString(requestBody));

        if (config.organizationId() != null && !config.organizationId().isBlank()) {
            httpRequestBuilder.header("OpenAI-Organization", config.organizationId());
        }

        try {
            HttpResponse<String> response = httpClient.send(
                httpRequestBuilder.build(),
                HttpResponse.BodyHandlers.ofString()
            );

            if (response.statusCode() == 429) {
                throw new ModelRateLimitException(
                    "Rate limit exceeded",
                    modelId,
                    "openai",
                    extractRetryAfter(response)
                );
            }

            if (response.statusCode() == 401) {
                throw new ModelException(
                    "Authentication failed: invalid API key",
                    modelId,
                    "openai"
                );
            }

            if (response.statusCode() >= 400) {
                throw new ModelException(
                    "HTTP " + response.statusCode() + ": " + response.body(),
                    modelId,
                    "openai"
                );
            }

            return parseResponse(response.body());

        } catch (ModelException e) {
            throw e;
        } catch (java.io.IOException e) {
            throw new ModelException("Network error communicating with model", modelId, "openai", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ModelException("Request interrupted", modelId, "openai", e);
        }
    }

    @Override
    public String modelId() {
        return modelId;
    }

    @Override
    public String providerId() {
        return "openai";
    }

    // ── Request body building (JsonGenerator) ──

    private String buildRequestBody(ChatRequest request) {
        try (StringWriter sw = new StringWriter()) {
            JsonGenerator g = JSON_FACTORY.createGenerator(sw);
            g.writeStartObject();
            g.writeStringField("model", modelId);

            // Messages
            g.writeFieldName("messages");
            writeMessages(g, request.messages());

            if (request.systemInstruction() != null) {
                g.writeStringField("system", request.systemInstruction());
            }
            if (request.temperature() != null) {
                g.writeNumberField("temperature", request.temperature());
            }
            if (request.maxTokens() != null) {
                g.writeNumberField("max_tokens", request.maxTokens());
            }
            if (request.tools() != null && !request.tools().isEmpty()) {
                g.writeFieldName("tools");
                writeTools(g, request.tools());
            }
            if (request.responseFormat() != null) {
                g.writeFieldName("response_format");
                writeResponseFormat(g, request.responseFormat());
            }

            g.writeEndObject();
            g.flush();
            return sw.toString();
        } catch (IOException e) {
            throw new IllegalStateException("Failed to build request body", e);
        }
    }

    private static void writeMessages(JsonGenerator g, List<Message> messages) throws IOException {
        g.writeStartArray();
        for (Message msg : messages) {
            g.writeStartObject();
            if (msg instanceof SystemMessage s) {
                g.writeStringField("role", "system");
                g.writeStringField("content", s.content());
            } else if (msg instanceof UserMessage u) {
                g.writeStringField("role", "user");
                g.writeStringField("content", u.content());
            } else if (msg instanceof AssistantMessage a) {
                g.writeStringField("role", "assistant");
                g.writeStringField("content", a.content());
                if (!a.toolCalls().isEmpty()) {
                    g.writeFieldName("tool_calls");
                    writeToolCallsOutput(g, a.toolCalls());
                }
            } else if (msg instanceof ToolMessage t) {
                g.writeStringField("role", "tool");
                g.writeStringField("tool_call_id", t.callId());
                g.writeStringField("content", t.content());
            }
            g.writeEndObject();
        }
        g.writeEndArray();
    }

    private static void writeToolCallsOutput(JsonGenerator g, List<ToolCall> toolCalls) throws IOException {
        g.writeStartArray();
        for (ToolCall tc : toolCalls) {
            g.writeStartObject();
            g.writeStringField("id", tc.callId());
            g.writeStringField("type", "function");
            g.writeObjectFieldStart("function");
            g.writeStringField("name", tc.toolId());
            g.writeStringField("arguments", mapToJson(tc.arguments()));
            g.writeEndObject();
            g.writeEndObject();
        }
        g.writeEndArray();
    }

    private static void writeTools(JsonGenerator g, List<ToolDescriptor> tools) throws IOException {
        g.writeStartArray();
        for (ToolDescriptor tool : tools) {
            g.writeStartObject();
            g.writeStringField("type", "function");
            g.writeObjectFieldStart("function");
            g.writeStringField("name", tool.id());
            g.writeStringField("description", tool.description());
            g.writeFieldName("parameters");
            writeValue(g, tool.inputSchema());
            g.writeEndObject();
            g.writeEndObject();
        }
        g.writeEndArray();
    }

    private static void writeResponseFormat(JsonGenerator g, ResponseFormat format) throws IOException {
        g.writeStartObject();
        switch (format.type()) {
            case TEXT -> g.writeStringField("type", "text");
            case JSON -> g.writeStringField("type", "json_object");
            case JSON_SCHEMA -> {
                g.writeStringField("type", "json_schema");
                g.writeObjectFieldStart("json_schema");
                g.writeFieldName("schema");
                g.writeRawValue(format.schema());
                g.writeEndObject();
            }
        }
        g.writeEndObject();
    }

    private static void writeValue(JsonGenerator g, Object value) throws IOException {
        if (value == null) {
            g.writeNull();
        } else if (value instanceof String s) {
            g.writeString(s);
        } else if (value instanceof Integer i) {
            g.writeNumber(i);
        } else if (value instanceof Long l) {
            g.writeNumber(l);
        } else if (value instanceof Double d) {
            g.writeNumber(d);
        } else if (value instanceof Float f) {
            g.writeNumber(f);
        } else if (value instanceof Boolean b) {
            g.writeBoolean(b);
        } else if (value instanceof Map<?, ?> m) {
            g.writeStartObject();
            for (var entry : m.entrySet()) {
                g.writeFieldName(entry.getKey().toString());
                writeValue(g, entry.getValue());
            }
            g.writeEndObject();
        } else {
            g.writeString(value.toString());
        }
    }

    // ── Response parsing (JsonParser) ──

    private ChatResponse parseResponse(String responseBody) {
        try (JsonParser p = JSON_FACTORY.createParser(responseBody)) {
            String content = null;
            String model = null;
            String finishReasonStr = null;
            TokenUsage usage = TokenUsage.empty();
            List<ToolCall> toolCalls = List.of();

            p.nextToken(); // START_OBJECT
            while (p.nextToken() != JsonToken.END_OBJECT) {
                String field = p.currentName();
                p.nextToken();

                switch (field) {
                    case "choices" -> {
                        if (p.currentToken() == JsonToken.START_ARRAY && p.nextToken() == JsonToken.START_OBJECT) {
                            while (p.nextToken() != JsonToken.END_OBJECT) {
                                String choiceField = p.currentName();
                                p.nextToken();
                                if ("message".equals(choiceField) && p.currentToken() == JsonToken.START_OBJECT) {
                                    var msgResult = parseMessageObject(p);
                                    content = msgResult.content;
                                    toolCalls = msgResult.toolCalls;
                                } else if ("finish_reason".equals(choiceField)) {
                                    finishReasonStr = p.getText();
                                } else {
                                    p.skipChildren();
                                }
                            }
                        } else {
                            p.skipChildren();
                        }
                    }
                    case "model" -> model = p.getText();
                    case "usage" -> usage = parseUsageObject(p);
                    default -> p.skipChildren();
                }
            }

            FinishReason finishReason = parseFinishReason(finishReasonStr);
            return ChatResponse.builder()
                .content(content != null ? content : "")
                .toolCalls(toolCalls)
                .finishReason(toolCalls.isEmpty() ? finishReason : FinishReason.TOOL_CALLS)
                .usage(usage)
                .model(model)
                .build();
        } catch (IOException e) {
            throw new IllegalStateException("Failed to parse response", e);
        }
    }

    private record MessageParseResult(String content, List<ToolCall> toolCalls) {}

    private static MessageParseResult parseMessageObject(JsonParser p) throws IOException {
        String content = null;
        List<ToolCall> toolCalls = List.of();

        while (p.nextToken() != JsonToken.END_OBJECT) {
            String field = p.currentName();
            p.nextToken();

            switch (field) {
                case "content" -> content = p.getText();
                case "tool_calls" -> toolCalls = parseToolCallsArray(p);
                default -> p.skipChildren();
            }
        }
        return new MessageParseResult(content, toolCalls);
    }

    private static List<ToolCall> parseToolCallsArray(JsonParser p) throws IOException {
        if (p.currentToken() != JsonToken.START_ARRAY) {
            p.skipChildren();
            return List.of();
        }

        List<ToolCall> calls = new ArrayList<>();
        while (p.nextToken() == JsonToken.START_OBJECT) {
            String id = null;
            String name = null;
            Map<String, Object> args = Map.of();

            while (p.nextToken() != JsonToken.END_OBJECT) {
                String field = p.currentName();
                p.nextToken();

                switch (field) {
                    case "id" -> id = p.getText();
                    case "function" -> {
                        if (p.currentToken() == JsonToken.START_OBJECT) {
                            while (p.nextToken() != JsonToken.END_OBJECT) {
                                String funcField = p.currentName();
                                p.nextToken();
                                if ("name".equals(funcField)) name = p.getText();
                                else if ("arguments".equals(funcField)) {
                                    String argsStr = p.getText();
                                    args = argsStr != null ? parseJsonMap(argsStr) : Map.of();
                                } else {
                                    p.skipChildren();
                                }
                            }
                        }
                    }
                    default -> p.skipChildren();
                }
            }

            if (id != null && name != null) {
                calls.add(ToolCall.of(id, name, args));
            }
        }
        return calls;
    }

    private static TokenUsage parseUsageObject(JsonParser p) throws IOException {
        if (p.currentToken() != JsonToken.START_OBJECT) {
            p.skipChildren();
            return TokenUsage.empty();
        }

        long promptTokens = 0;
        long completionTokens = 0;
        long totalTokens = 0;

        while (p.nextToken() != JsonToken.END_OBJECT) {
            String field = p.currentName();
            p.nextToken();

            switch (field) {
                case "prompt_tokens" -> promptTokens = p.getLongValue();
                case "completion_tokens" -> completionTokens = p.getLongValue();
                case "total_tokens" -> totalTokens = p.getLongValue();
                default -> p.skipChildren();
            }
        }

        if (totalTokens == 0) totalTokens = promptTokens + completionTokens;
        return TokenUsage.of(promptTokens, completionTokens, totalTokens);
    }

    // ── JSON map parsing ──

    static Map<String, Object> parseJsonMap(String json) {
        Map<String, Object> map = new LinkedHashMap<>();
        try (JsonParser p = JSON_FACTORY.createParser(json)) {
            if (p.nextToken() != JsonToken.START_OBJECT) return map;
            return readObject(p);
        } catch (IOException e) {
            // Return partial result on parse error
        }
        return map;
    }

    // G3-6: recursive object reader for nested structures
    private static Map<String, Object> readObject(JsonParser p)
        throws IOException {
        Map<String, Object> map = new LinkedHashMap<>();
        while (p.nextToken() != JsonToken.END_OBJECT) {
            String key = p.currentName();
            p.nextToken();
            map.put(key, readValue(p));
        }
        return map;
    }

    // G3-6: recursive value reader handling all JSON types
    private static Object readValue(JsonParser p) throws IOException {
        return switch (p.currentToken()) {
            case VALUE_STRING -> p.getText();
            case VALUE_NUMBER_INT -> {
                long l = p.getLongValue();
                if (l >= Integer.MIN_VALUE && l <= Integer.MAX_VALUE) {
                    yield (int) l;
                }
                yield l;
            }
            case VALUE_NUMBER_FLOAT -> p.getDoubleValue();
            case VALUE_TRUE -> true;
            case VALUE_FALSE -> false;
            case VALUE_NULL -> null;
            case START_OBJECT -> readObject(p);
            case START_ARRAY -> {
                List<Object> list = new ArrayList<>();
                while (p.nextToken() != JsonToken.END_ARRAY) {
                    list.add(readValue(p));
                }
                yield List.copyOf(list);
            }
            default -> {
                p.skipChildren();
                yield null;
            }
        };
    }

    private static String readRawValue(JsonParser p) throws IOException {
        StringWriter sw = new StringWriter();
        try (JsonGenerator g = JSON_FACTORY.createGenerator(sw)) {
            g.copyCurrentStructure(p);
        }
        return sw.toString();
    }

    // ── JSON output helpers ──

    static String mapToJson(Map<String, Object> map) {
        if (map == null || map.isEmpty()) return "{}";
        try (StringWriter sw = new StringWriter()) {
            JsonGenerator g = JSON_FACTORY.createGenerator(sw);
            writeValue(g, map);
            g.flush();
            return sw.toString();
        } catch (IOException e) {
            throw new IllegalStateException("Failed to serialize map to JSON", e);
        }
    }

    // ── Misc helpers ──

    private static FinishReason parseFinishReason(String reason) {
        if (reason == null) return FinishReason.UNKNOWN;
        return switch (reason) {
            case "stop" -> FinishReason.STOP;
            case "length" -> FinishReason.LENGTH;
            case "tool_calls" -> FinishReason.TOOL_CALLS;
            case "content_filter" -> FinishReason.CONTENT_FILTER;
            default -> FinishReason.UNKNOWN;
        };
    }

    private Long extractRetryAfter(HttpResponse<String> response) {
        return response.headers()
            .firstValue("Retry-After")
            .map(s -> {
                try { return Long.parseLong(s); } catch (NumberFormatException e) { return null; }
            })
            .orElse(null);
    }
}
