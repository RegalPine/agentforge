package io.github.regalpine.agentforge.provider.openai;

import com.fasterxml.jackson.core.*;
import io.github.regalpine.agentforge.model.*;

import java.io.IOException;
import java.io.StringWriter;
import java.util.*;

/**
 * JSON building and parsing utilities for OpenAI API.
 * <p>
 * Uses {@code jackson-core} (streaming API) for robust JSON handling,
 * including proper Unicode escaping, nested JSON, and edge cases.
 */
final class OpenAiJson {

    private static final JsonFactory JSON_FACTORY = new JsonFactory();

    private OpenAiJson() {}

    // ── Request body building ──

    static String buildChatRequestBody(String modelId, ChatRequest request, boolean stream) {
        try (StringWriter sw = new StringWriter()) {
            JsonGenerator g = JSON_FACTORY.createGenerator(sw);
            g.writeStartObject();
            g.writeStringField("model", modelId);

            // Messages
            g.writeFieldName("messages");
            writeMessages(g, request.messages());

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
            if (stream) {
                g.writeBooleanField("stream", true);
                g.writeObjectFieldStart("stream_options");
                g.writeBooleanField("include_usage", true);
                g.writeEndObject();
            }

            // Provider-specific options
            for (var entry : request.providerOptions().entrySet()) {
                g.writeFieldName(entry.getKey());
                writeValue(g, entry.getValue());
            }

            g.writeEndObject();
            g.flush();
            return sw.toString();
        } catch (IOException e) {
            throw new IllegalStateException("Failed to build chat request body", e);
        }
    }

    static String buildEmbeddingRequestBody(String modelId, EmbeddingRequest request) {
        try (StringWriter sw = new StringWriter()) {
            JsonGenerator g = JSON_FACTORY.createGenerator(sw);
            g.writeStartObject();
            g.writeStringField("model", modelId);

            g.writeFieldName("input");
            List<String> texts = request.texts();
            if (texts.size() == 1) {
                g.writeString(texts.get(0));
            } else {
                g.writeStartArray();
                for (String text : texts) {
                    g.writeString(text);
                }
                g.writeEndArray();
            }

            g.writeEndObject();
            g.flush();
            return sw.toString();
        } catch (IOException e) {
            throw new IllegalStateException("Failed to build embedding request body", e);
        }
    }

    // ── Response parsing ──

    static ChatResponse parseChatResponse(String responseBody) {
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
                        if (p.currentToken() == JsonToken.START_ARRAY) {
                            while (p.nextToken() == JsonToken.START_OBJECT) {
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
            throw new IllegalStateException("Failed to parse chat response", e);
        }
    }

    static EmbeddingResponse parseEmbeddingResponse(String responseBody) {
        try (JsonParser p = JSON_FACTORY.createParser(responseBody)) {
            List<EmbeddingVector> vectors = new ArrayList<>();
            TokenUsage usage = TokenUsage.empty();

            p.nextToken();
            while (p.nextToken() != JsonToken.END_OBJECT) {
                String field = p.currentName();
                p.nextToken();

                switch (field) {
                    case "data" -> {
                        if (p.currentToken() == JsonToken.START_ARRAY) {
                            while (p.nextToken() == JsonToken.START_OBJECT) {
                                vectors.add(parseEmbeddingVectorObject(p));
                            }
                        }
                    }
                    case "usage" -> usage = parseUsageObject(p);
                    default -> p.skipChildren();
                }
            }

            return new EmbeddingResponse(vectors, usage, null, null);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to parse embedding response", e);
        }
    }

    // ── SSE event parsing ──

    static ChatEvent parseStreamEvent(String data) {
        if (data.equals("[DONE]")) {
            return new ChatEvent.ChatStreamCompleted(FinishReason.STOP);
        }

        try (JsonParser p = JSON_FACTORY.createParser(data)) {
            String deltaContent = null;
            String finishReason = null;
            TokenUsage usage = null;
            List<ToolCallDeltaInfo> toolCallDeltas = new ArrayList<>();

            p.nextToken();
            while (p.nextToken() != JsonToken.END_OBJECT) {
                String field = p.currentName();
                p.nextToken();

                switch (field) {
                    case "choices" -> {
                        if (p.currentToken() == JsonToken.START_ARRAY) {
                            while (p.nextToken() == JsonToken.START_OBJECT) {
                                while (p.nextToken() != JsonToken.END_OBJECT) {
                                    String choiceField = p.currentName();
                                    p.nextToken();
                                    if ("delta".equals(choiceField) && p.currentToken() == JsonToken.START_OBJECT) {
                                        var deltaResult = parseDeltaObject(p);
                                        deltaContent = deltaResult.content;
                                        if (deltaResult.toolCalls != null) {
                                            toolCallDeltas.addAll(deltaResult.toolCalls);
                                        }
                                    } else if ("finish_reason".equals(choiceField)) {
                                        finishReason = p.getText();
                                    } else {
                                        p.skipChildren();
                                    }
                                }
                            }
                        } else {
                            p.skipChildren();
                        }
                    }
                    case "usage" -> usage = parseUsageObject(p);
                    default -> p.skipChildren();
                }
            }

            // Usage chunk
            if (usage != null) {
                return new ChatEvent.UsageReceived(usage);
            }

            // Finish reason
            if (finishReason != null && !"null".equals(finishReason)) {
                FinishReason fr = parseFinishReason(finishReason);
                if (deltaContent != null && !deltaContent.isEmpty()) {
                    return new ChatEvent.TextDelta(deltaContent);
                }
                return new ChatEvent.ChatStreamCompleted(fr);
            }

            // Tool call deltas
            if (!toolCallDeltas.isEmpty()) {
                for (ToolCallDeltaInfo tcd : toolCallDeltas) {
                    if (tcd.id != null && tcd.name != null) {
                        return new ChatEvent.ToolCallStarted(tcd.id, tcd.name);
                    }
                    if (tcd.arguments != null && !tcd.arguments.isEmpty()) {
                        return new ChatEvent.ToolCallDelta(
                            tcd.id != null ? tcd.id : "unknown", tcd.arguments
                        );
                    }
                }
            }

            // Regular text delta
            if (deltaContent != null && !deltaContent.isEmpty()) {
                return new ChatEvent.TextDelta(deltaContent);
            }

            return null;
        } catch (IOException e) {
            return null;
        }
    }

    // ── Message building (JsonGenerator) ──

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
            g.writeNullField(null);
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

    // ── Parsing helpers (JsonParser) ──

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

    private static EmbeddingVector parseEmbeddingVectorObject(JsonParser p) throws IOException {
        int index = 0;
        float[] embedding = new float[0];

        while (p.nextToken() != JsonToken.END_OBJECT) {
            String field = p.currentName();
            p.nextToken();

            switch (field) {
                case "index" -> index = p.getIntValue();
                case "embedding" -> {
                    if (p.currentToken() == JsonToken.START_ARRAY) {
                        List<Float> values = new ArrayList<>();
                        while (p.nextToken() == JsonToken.VALUE_NUMBER_FLOAT
                            || p.currentToken() == JsonToken.VALUE_NUMBER_INT) {
                            values.add(p.getFloatValue());
                        }
                        embedding = new float[values.size()];
                        for (int i = 0; i < values.size(); i++) {
                            embedding[i] = values.get(i);
                        }
                    }
                }
                default -> p.skipChildren();
            }
        }

        return new EmbeddingVector(index, embedding);
    }

    // ── Delta parsing for streaming ──

    private record DeltaParseResult(String content, List<ToolCallDeltaInfo> toolCalls) {}
    private record ToolCallDeltaInfo(String id, String name, String arguments) {}

    private static DeltaParseResult parseDeltaObject(JsonParser p) throws IOException {
        String content = null;
        List<ToolCallDeltaInfo> toolCallDeltas = null;

        while (p.nextToken() != JsonToken.END_OBJECT) {
            String field = p.currentName();
            p.nextToken();

            switch (field) {
                case "content" -> content = p.getText();
                case "tool_calls" -> {
                    toolCallDeltas = new ArrayList<>();
                    if (p.currentToken() == JsonToken.START_ARRAY) {
                        while (p.nextToken() == JsonToken.START_OBJECT) {
                            String idx = null;
                            String id = null;
                            String name = null;
                            String arguments = null;

                            while (p.nextToken() != JsonToken.END_OBJECT) {
                                String tcField = p.currentName();
                                p.nextToken();

                                switch (tcField) {
                                    case "index" -> idx = p.getText();
                                    case "id" -> id = p.getText();
                                    case "function" -> {
                                        if (p.currentToken() == JsonToken.START_OBJECT) {
                                            while (p.nextToken() != JsonToken.END_OBJECT) {
                                                String funcField = p.currentName();
                                                p.nextToken();
                                                if ("name".equals(funcField)) name = p.getText();
                                                else if ("arguments".equals(funcField)) arguments = p.getText();
                                                else p.skipChildren();
                                            }
                                        }
                                    }
                                    default -> p.skipChildren();
                                }
                            }

                            toolCallDeltas.add(new ToolCallDeltaInfo(id, name, arguments));
                        }
                    }
                }
                default -> p.skipChildren();
            }
        }

        return new DeltaParseResult(content, toolCallDeltas);
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
}
