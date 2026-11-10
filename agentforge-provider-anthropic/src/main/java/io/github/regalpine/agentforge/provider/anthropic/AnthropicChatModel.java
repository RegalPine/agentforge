package io.github.regalpine.agentforge.provider.anthropic;

import io.github.regalpine.agentforge.model.*;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.*;
import java.util.concurrent.Flow;

/**
 * Anthropic Claude ChatModel implementation.
 * <p>
 * Uses the Anthropic Messages API with support for Tool Use and Streaming.
 */
public class AnthropicChatModel implements ChatModel {

    private final AnthropicConfig config;
    private final HttpClient httpClient;
    private final String modelId;

    public AnthropicChatModel(AnthropicConfig config, String modelId) {
        this.config = Objects.requireNonNull(config, "config");
        this.modelId = Objects.requireNonNull(modelId, "modelId");
        this.httpClient = HttpClient.newBuilder()
            .connectTimeout(config.connectTimeout())
            .build();
    }

    @Override
    public ChatResponse chat(ChatRequest request) {
        Objects.requireNonNull(request, "request");
        String requestBody = buildRequestBody(request, false);
        String responseBody = executeRequest(requestBody);
        return parseResponse(responseBody);
    }

    @Override
    public Flow.Publisher<ChatEvent> stream(ChatRequest request) {
        Objects.requireNonNull(request, "request");
        return new AnthropicStreamingPublisher(config, httpClient, modelId, request);
    }

    @Override
    public String modelId() { return modelId; }

    @Override
    public String providerId() { return "anthropic"; }

    private String executeRequest(String requestBody) {
        HttpRequest httpRequest = HttpRequest.newBuilder()
            .uri(URI.create(config.baseUrl() + "/messages"))
            .header("Content-Type", "application/json")
            .header("x-api-key", config.apiKey())
            .header("anthropic-version", config.anthropicVersion())
            .timeout(config.readTimeout())
            .POST(HttpRequest.BodyPublishers.ofString(requestBody))
            .build();

        try {
            HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 429) {
                throw new ModelRateLimitException("Rate limit exceeded", modelId, "anthropic");
            }
            if (response.statusCode() == 401) {
                throw new ModelException("Authentication failed", modelId, "anthropic");
            }
            if (response.statusCode() >= 400) {
                throw new ModelException("HTTP " + response.statusCode() + ": " + response.body(), modelId, "anthropic");
            }
            return response.body();
        } catch (ModelException e) {
            throw e;
        } catch (java.io.IOException e) {
            throw new ModelException("Network error communicating with Anthropic", modelId, "anthropic", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ModelException("Request interrupted", modelId, "anthropic", e);
        }
    }

    String buildRequestBody(ChatRequest request, boolean stream) {
        StringBuilder json = new StringBuilder();
        json.append("{");
        json.append("\"model\":\"").append(esc(modelId)).append("\"");

        // Build messages and extract system message
        List<Message> messages = request.messages();
        String systemMsg = request.systemInstruction();

        // Extract system message from messages if not already set
        if (systemMsg == null || systemMsg.isEmpty()) {
            for (Message msg : messages) {
                if (msg instanceof SystemMessage s) {
                    systemMsg = s.content();
                    break;
                }
            }
        }

        // Anthropic uses separate system parameter
        if (systemMsg != null && !systemMsg.isEmpty()) {
            json.append(",\"system\":\"").append(esc(systemMsg)).append("\"");
        }

        // Build messages array (no system messages in the array for Anthropic)
        json.append(",\"messages\":[");
        boolean first = true;
        for (Message msg : messages) {
            if (msg instanceof SystemMessage) continue; // Skip system messages
            if (!first) json.append(",");
            first = false;
            json.append(messageToJson(msg));
        }
        json.append("]");

        if (request.maxTokens() != null) {
            json.append(",\"max_tokens\":").append(request.maxTokens());
        } else {
            json.append(",\"max_tokens\":4096"); // Anthropic requires max_tokens
        }

        if (request.temperature() != null) {
            json.append(",\"temperature\":").append(request.temperature());
        }

        if (request.tools() != null && !request.tools().isEmpty()) {
            json.append(",\"tools\":").append(buildTools(request.tools()));
        }

        if (stream) {
            json.append(",\"stream\":true");
        }

        json.append("}");
        return json.toString();
    }

    private String messageToJson(Message msg) {
        if (msg instanceof UserMessage u) {
            return "{\"role\":\"user\",\"content\":\"" + esc(u.content()) + "\"}";
        } else if (msg instanceof AssistantMessage a) {
            StringBuilder sb = new StringBuilder();
            sb.append("{\"role\":\"assistant\",\"content\":\"").append(esc(a.content())).append("\"");
            if (!a.toolCalls().isEmpty()) {
                // Anthropic tool_use blocks
                sb.append("}");
                return sb.toString();
            }
            sb.append("}");
            return sb.toString();
        } else if (msg instanceof ToolMessage t) {
            return "{\"role\":\"user\",\"content\":[{\"type\":\"tool_result\",\"tool_use_id\":\""
                + esc(t.callId()) + "\",\"content\":\"" + esc(t.content()) + "\"}]}";
        }
        throw new IllegalArgumentException("Unknown message type: " + msg.getClass());
    }

    private String buildTools(List<ToolDescriptor> tools) {
        StringBuilder json = new StringBuilder("[");
        for (int i = 0; i < tools.size(); i++) {
            if (i > 0) json.append(",");
            ToolDescriptor tool = tools.get(i);
            json.append("{\"name\":\"").append(esc(tool.id())).append("\"");
            json.append(",\"description\":\"").append(esc(tool.description())).append("\"");
            json.append(",\"input_schema\":").append(mapToJson(tool.inputSchema()));
            json.append("}");
        }
        json.append("]");
        return json.toString();
    }

    ChatResponse parseResponse(String responseBody) {
        // Parse Anthropic Messages API response
        String stopReason = extractStringField(responseBody, "stop_reason");
        String model = extractStringField(responseBody, "model");

        // Extract content blocks
        String content = extractTextContent(responseBody);
        List<ToolCall> toolCalls = extractToolUseBlocks(responseBody);
        TokenUsage usage = parseUsage(responseBody);

        FinishReason finishReason = switch (stopReason != null ? stopReason : "") {
            case "end_turn" -> FinishReason.STOP;
            case "max_tokens" -> FinishReason.LENGTH;
            case "tool_use" -> FinishReason.TOOL_CALLS;
            default -> FinishReason.UNKNOWN;
        };

        return ChatResponse.builder()
            .content(content != null ? content : "")
            .toolCalls(toolCalls)
            .finishReason(toolCalls.isEmpty() ? finishReason : FinishReason.TOOL_CALLS)
            .usage(usage)
            .model(model)
            .build();
    }

    private String extractTextContent(String json) {
        String contentKey = "\"content\"";
        int idx = json.indexOf(contentKey);
        if (idx < 0) return null;
        String sub = json.substring(idx);
        int arrStart = sub.indexOf('[');
        if (arrStart < 0) return null;

        StringBuilder text = new StringBuilder();
        int depth = 0;
        int objStart = -1;
        for (int i = arrStart; i < sub.length(); i++) {
            char c = sub.charAt(i);
            if (c == '{') { if (depth == 1) objStart = i; depth++; }
            else if (c == '}') {
                depth--;
                if (depth == 1 && objStart >= 0) {
                    String block = sub.substring(objStart, i + 1);
                    String type = extractStringField(block, "type");
                    if ("text".equals(type)) {
                        String textContent = extractStringField(block, "text");
                        if (textContent != null) text.append(textContent);
                    }
                    objStart = -1;
                }
            } else if (c == ']' && depth == 0) break;
        }
        return text.toString();
    }

    private List<ToolCall> extractToolUseBlocks(String json) {
        String contentKey = "\"content\"";
        int idx = json.indexOf(contentKey);
        if (idx < 0) return List.of();
        String sub = json.substring(idx);
        int arrStart = sub.indexOf('[');
        if (arrStart < 0) return List.of();

        List<ToolCall> calls = new ArrayList<>();
        int depth = 0;
        int objStart = -1;
        for (int i = arrStart; i < sub.length(); i++) {
            char c = sub.charAt(i);
            if (c == '{') { if (depth == 1) objStart = i; depth++; }
            else if (c == '}') {
                depth--;
                if (depth == 1 && objStart >= 0) {
                    String block = sub.substring(objStart, i + 1);
                    String type = extractStringField(block, "type");
                    if ("tool_use".equals(type)) {
                        String id = extractStringField(block, "id");
                        String name = extractStringField(block, "name");
                        if (id != null && name != null) {
                            Map<String, Object> input = parseInputObject(block);
                            calls.add(ToolCall.of(id, name, input));
                        }
                    }
                    objStart = -1;
                }
            } else if (c == ']' && depth == 0) break;
        }
        return calls;
    }

    private TokenUsage parseUsage(String json) {
        long inputTokens = extractLongField(json, "input_tokens");
        long outputTokens = extractLongField(json, "output_tokens");
        return TokenUsage.of(inputTokens, outputTokens);
    }

    /**
     * Parse the "input" field from a tool_use block JSON.
     * Returns an empty map if input is missing or cannot be parsed.
     */
    private Map<String, Object> parseInputObject(String blockJson) {
        String inputKey = "\"input\"";
        int idx = blockJson.indexOf(inputKey);
        if (idx < 0) return Map.of();
        int colonIdx = blockJson.indexOf(':', idx + inputKey.length());
        if (colonIdx < 0) return Map.of();
        // Skip whitespace
        int start = colonIdx + 1;
        while (start < blockJson.length() && blockJson.charAt(start) == ' ') start++;
        if (start >= blockJson.length()) return Map.of();
        if (blockJson.charAt(start) == '{') {
            // Find matching closing brace
            int depth = 0;
            int end = start;
            for (int i = start; i < blockJson.length(); i++) {
                char c = blockJson.charAt(i);
                if (c == '{') depth++;
                else if (c == '}') { depth--; if (depth == 0) { end = i + 1; break; } }
            }
            String inputJson = blockJson.substring(start, end);
            return parseJsonObject(inputJson);
        }
        return Map.of();
    }

    /**
     * Minimal recursive JSON object parser.
     * Handles strings, numbers, booleans, null, nested objects and arrays.
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> parseJsonObject(String json) {
        json = json.trim();
        if (json.isEmpty() || json.charAt(0) != '{') return Map.of();
        Map<String, Object> result = new LinkedHashMap<>();
        int pos = 1; // skip opening {
        while (pos < json.length()) {
            // Skip whitespace
            while (pos < json.length() && Character.isWhitespace(json.charAt(pos))) pos++;
            if (pos >= json.length() || json.charAt(pos) == '}') break;
            // Parse key
            if (json.charAt(pos) != '"') break;
            pos++;
            int keyStart = pos;
            while (pos < json.length() && json.charAt(pos) != '"') {
                if (json.charAt(pos) == '\\') pos++;
                pos++;
            }
            String key = json.substring(keyStart, pos);
            pos++; // skip closing "
            // Skip colon
            while (pos < json.length() && (json.charAt(pos) == ' ' || json.charAt(pos) == ':')) pos++;
            // Parse value
            Object[] valueAndEnd = parseJsonValue(json, pos);
            result.put(key, valueAndEnd[0]);
            pos = (int) valueAndEnd[1];
            // Skip comma
            while (pos < json.length() && (json.charAt(pos) == ' ' || json.charAt(pos) == ',')) pos++;
        }
        return result;
    }

    /** Returns [value, newPos]. */
    private Object[] parseJsonValue(String json, int pos) {
        while (pos < json.length() && Character.isWhitespace(json.charAt(pos))) pos++;
        if (pos >= json.length()) return new Object[]{null, pos};
        char c = json.charAt(pos);
        if (c == '"') return parseJsonString(json, pos);
        if (c == '{') return parseJsonObjectValue(json, pos);
        if (c == '[') return parseJsonArray(json, pos);
        if (c == 't' && json.startsWith("true", pos)) return new Object[]{Boolean.TRUE, pos + 4};
        if (c == 'f' && json.startsWith("false", pos)) return new Object[]{Boolean.FALSE, pos + 5};
        if (c == 'n' && json.startsWith("null", pos)) return new Object[]{null, pos + 4};
        if (c == '-' || Character.isDigit(c)) return parseJsonNumber(json, pos);
        return new Object[]{null, pos};
    }

    private Object[] parseJsonString(String json, int pos) {
        pos++; // skip opening "
        StringBuilder sb = new StringBuilder();
        while (pos < json.length()) {
            char ch = json.charAt(pos);
            if (ch == '\\' && pos + 1 < json.length()) {
                char next = json.charAt(pos + 1);
                sb.append(switch (next) { case '"' -> '"'; case '\\' -> '\\'; case 'n' -> '\n'; case 't' -> '\t'; case 'r' -> '\r'; default -> next; });
                pos += 2;
            } else if (ch == '"') {
                return new Object[]{sb.toString(), pos + 1};
            } else {
                sb.append(ch);
                pos++;
            }
        }
        return new Object[]{sb.toString(), pos};
    }

    private Object[] parseJsonObjectValue(String json, int pos) {
        int start = pos;
        int depth = 0;
        for (int i = pos; i < json.length(); i++) {
            char ch = json.charAt(i);
            if (ch == '{') depth++;
            else if (ch == '}') { depth--; if (depth == 0) { pos = i + 1; break; } }
        }
        String sub = json.substring(start, pos);
        return new Object[]{parseJsonObject(sub), pos};
    }

    private Object[] parseJsonArray(String json, int pos) {
        pos++; // skip [
        List<Object> list = new ArrayList<>();
        while (pos < json.length()) {
            while (pos < json.length() && Character.isWhitespace(json.charAt(pos))) pos++;
            if (pos >= json.length() || json.charAt(pos) == ']') { pos++; break; }
            Object[] val = parseJsonValue(json, pos);
            list.add(val[0]);
            pos = (int) val[1];
            while (pos < json.length() && (json.charAt(pos) == ' ' || json.charAt(pos) == ',')) pos++;
        }
        return new Object[]{list, pos};
    }

    private Object[] parseJsonNumber(String json, int pos) {
        int start = pos;
        if (json.charAt(pos) == '-') pos++;
        while (pos < json.length() && Character.isDigit(json.charAt(pos))) pos++;
        boolean isFloat = false;
        if (pos < json.length() && json.charAt(pos) == '.') { isFloat = true; pos++; while (pos < json.length() && Character.isDigit(json.charAt(pos))) pos++; }
        if (pos < json.length() && (json.charAt(pos) == 'e' || json.charAt(pos) == 'E')) { isFloat = true; pos++; if (pos < json.length() && (json.charAt(pos) == '+' || json.charAt(pos) == '-')) pos++; while (pos < json.length() && Character.isDigit(json.charAt(pos))) pos++; }
        String numStr = json.substring(start, pos);
        if (isFloat) return new Object[]{Double.parseDouble(numStr), pos};
        try {
            long val = Long.parseLong(numStr);
            if (val >= Integer.MIN_VALUE && val <= Integer.MAX_VALUE) return new Object[]{(int) val, pos};
            return new Object[]{val, pos};
        } catch (NumberFormatException e) {
            return new Object[]{Double.parseDouble(numStr), pos};
        }
    }

    private String extractStringField(String json, String field) {
        String key = "\"" + field + "\":";
        int keyIdx = json.indexOf(key);
        if (keyIdx < 0) return null;
        int vs = keyIdx + key.length();
        while (vs < json.length() && json.charAt(vs) == ' ') vs++;
        if (vs >= json.length()) return null;
        if (json.charAt(vs) == '"') {
            int ve = vs + 1;
            while (ve < json.length()) {
                if (json.charAt(ve) == '\\') { ve += 2; continue; }
                if (json.charAt(ve) == '"') break;
                ve++;
            }
            return json.substring(vs + 1, ve);
        } else if (json.startsWith("null", vs)) return null;
        return null;
    }

    private long extractLongField(String json, String field) {
        String key = "\"" + field + "\":";
        int keyIdx = json.indexOf(key);
        if (keyIdx < 0) return 0;
        int vs = keyIdx + key.length();
        while (vs < json.length() && json.charAt(vs) == ' ') vs++;
        if (vs >= json.length()) return 0;
        int ve = vs;
        while (ve < json.length() && (Character.isDigit(json.charAt(ve)) || json.charAt(ve) == '-')) ve++;
        if (ve == vs) return 0;
        try { return Long.parseLong(json.substring(vs, ve)); } catch (NumberFormatException e) { return 0; }
    }

    private String mapToJson(Map<String, Object> map) {
        if (map == null || map.isEmpty()) return "{}";
        StringBuilder json = new StringBuilder("{");
        boolean first = true;
        for (var entry : map.entrySet()) {
            if (!first) json.append(",");
            first = false;
            json.append("\"").append(esc(entry.getKey())).append("\":");
            Object v = entry.getValue();
            if (v == null) json.append("null");
            else if (v instanceof String s) json.append("\"").append(esc(s)).append("\"");
            else if (v instanceof Number || v instanceof Boolean) json.append(v);
            else json.append("\"").append(esc(v.toString())).append("\"");
        }
        json.append("}");
        return json.toString();
    }

    private String esc(String s) {
        if (s == null) return "";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '"' -> sb.append("\\\"");
                case '\\' -> sb.append("\\\\");
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                case '\t' -> sb.append("\\t");
                default -> { if (c < 0x20) sb.append(String.format("\\u%04x", (int) c)); else sb.append(c); }
            }
        }
        return sb.toString();
    }
}
