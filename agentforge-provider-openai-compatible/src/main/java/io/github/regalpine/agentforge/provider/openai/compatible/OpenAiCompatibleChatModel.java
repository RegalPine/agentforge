package io.github.regalpine.agentforge.provider.openai.compatible;

import com.fasterxml.jackson.core.*;
import io.github.regalpine.agentforge.model.*;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.*;
import java.util.concurrent.Flow;

/**
 * OpenAI-Compatible ChatModel implementation.
 * <p>
 * Uses the OpenAI Chat Completions API protocol, allowing connection to
 * any compatible service (vLLM, LM Studio, enterprise gateways).
 */
public class OpenAiCompatibleChatModel implements ChatModel {

    private final OpenAiCompatibleConfig config;
    private final HttpClient httpClient;
    private final String modelId;

    public OpenAiCompatibleChatModel(OpenAiCompatibleConfig config, String modelId) {
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
        return new OpenAiCompatibleStreamingPublisher(config, httpClient, modelId, request);
    }

    @Override
    public String modelId() { return modelId; }

    @Override
    public String providerId() { return config.providerName(); }

    String buildRequestBody(ChatRequest request, boolean stream) {
        StringBuilder json = new StringBuilder();
        json.append("{");
        json.append("\"model\":\"").append(esc(modelId)).append("\"");
        json.append(",\"messages\":").append(buildMessages(request.messages()));

        if (request.temperature() != null) json.append(",\"temperature\":").append(request.temperature());
        if (request.maxTokens() != null) json.append(",\"max_tokens\":").append(request.maxTokens());

        if (request.tools() != null && !request.tools().isEmpty()) {
            json.append(",\"tools\":").append(buildTools(request.tools()));
        }
        if (request.responseFormat() != null) {
            json.append(",\"response_format\":").append(buildResponseFormat(request.responseFormat()));
        }
        if (stream) {
            json.append(",\"stream\":true");
        }

        if (!request.providerOptions().isEmpty()) {
            for (var entry : request.providerOptions().entrySet()) {
                json.append(",\"").append(esc(entry.getKey())).append("\":").append(valueToJson(entry.getValue()));
            }
        }

        json.append("}");
        return json.toString();
    }

    private String buildMessages(List<Message> messages) {
        StringBuilder json = new StringBuilder("[");
        for (int i = 0; i < messages.size(); i++) {
            if (i > 0) json.append(",");
            Message msg = messages.get(i);
            if (msg instanceof SystemMessage s) {
                json.append("{\"role\":\"system\",\"content\":\"").append(esc(s.content())).append("\"}");
            } else if (msg instanceof UserMessage u) {
                json.append("{\"role\":\"user\",\"content\":\"").append(esc(u.content())).append("\"}");
            } else if (msg instanceof AssistantMessage a) {
                json.append("{\"role\":\"assistant\",\"content\":\"").append(esc(a.content())).append("\"}");
            } else if (msg instanceof ToolMessage t) {
                json.append("{\"role\":\"tool\",\"tool_call_id\":\"").append(esc(t.callId())).append("\",\"content\":\"").append(esc(t.content())).append("\"}");
            }
        }
        json.append("]");
        return json.toString();
    }

    private String buildTools(List<ToolDescriptor> tools) {
        StringBuilder json = new StringBuilder("[");
        for (int i = 0; i < tools.size(); i++) {
            if (i > 0) json.append(",");
            ToolDescriptor tool = tools.get(i);
            json.append("{\"type\":\"function\",\"function\":{\"name\":\"").append(esc(tool.id())).append("\"");
            json.append(",\"description\":\"").append(esc(tool.description())).append("\"");
            json.append(",\"parameters\":").append(mapToJson(tool.inputSchema())).append("}}");
        }
        json.append("]");
        return json.toString();
    }

    private String buildResponseFormat(ResponseFormat format) {
        return switch (format.type()) {
            case TEXT -> "{\"type\":\"text\"}";
            case JSON -> "{\"type\":\"json_object\"}";
            case JSON_SCHEMA -> "{\"type\":\"json_schema\",\"json_schema\":{\"schema\":" + format.schema() + "}}";
        };
    }

    private String executeRequest(String requestBody) {
        HttpRequest.Builder builder = HttpRequest.newBuilder()
            .uri(URI.create(config.baseUrl() + "/chat/completions"))
            .header("Content-Type", "application/json")
            .timeout(config.readTimeout())
            .POST(HttpRequest.BodyPublishers.ofString(requestBody));

        if (!config.apiKey().isEmpty()) {
            builder.header("Authorization", "Bearer " + config.apiKey());
        }

        try {
            HttpResponse<String> response = httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 429) {
                throw new ModelRateLimitException("Rate limit exceeded", modelId, config.providerName());
            }
            if (response.statusCode() >= 400) {
                throw new ModelException("HTTP " + response.statusCode() + ": " + response.body(), modelId, config.providerName());
            }
            return response.body();
        } catch (ModelException e) {
            throw e;
        } catch (java.io.IOException e) {
            throw new ModelException("Network error", modelId, config.providerName(), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ModelException("Request interrupted", modelId, config.providerName(), e);
        }
    }

    ChatResponse parseResponse(String responseBody) {
        String content = extractStringInMessage(responseBody, "content");
        String model = extractTopLevelString(responseBody, "model");
        String finishStr = extractFinishReason(responseBody);
        FinishReason finishReason = parseFinishReason(finishStr);
        TokenUsage usage = parseUsage(responseBody);
        List<ToolCall> toolCalls = parseToolCalls(responseBody);

        return ChatResponse.builder()
            .content(content != null ? content : "")
            .toolCalls(toolCalls)
            .finishReason(toolCalls.isEmpty() ? finishReason : FinishReason.TOOL_CALLS)
            .usage(usage)
            .model(model)
            .build();
    }

    // --- Parsing helpers (same as OpenAI provider) ---

    private String extractStringInMessage(String json, String field) {
        String msgKey = "\"message\"";
        int idx = json.indexOf(msgKey);
        if (idx < 0) return extractField(json, field);
        String sub = json.substring(idx);
        int objStart = sub.indexOf('{');
        if (objStart < 0) return null;
        return extractField(sub.substring(objStart), field);
    }

    private String extractTopLevelString(String json, String field) {
        return extractField(json, field);
    }

    private String extractField(String json, String field) {
        String key = "\"" + field + "\":";
        int idx = json.indexOf(key);
        if (idx < 0) return null;
        int vs = idx + key.length();
        while (vs < json.length() && json.charAt(vs) == ' ') vs++;
        if (vs >= json.length()) return null;
        if (json.charAt(vs) == '"') {
            int ve = vs + 1;
            while (ve < json.length()) { if (json.charAt(ve) == '\\') { ve += 2; continue; } if (json.charAt(ve) == '"') break; ve++; }
            return json.substring(vs + 1, ve);
        } else if (json.startsWith("null", vs)) return null;
        return null;
    }

    private String extractFinishReason(String json) {
        String key = "\"finish_reason\":";
        int idx = json.indexOf(key);
        if (idx < 0) return null;
        int vs = idx + key.length();
        while (vs < json.length() && json.charAt(vs) == ' ') vs++;
        if (vs >= json.length() || json.charAt(vs) != '"') return null;
        int ve = vs + 1;
        while (ve < json.length() && json.charAt(ve) != '"') { if (json.charAt(ve) == '\\') ve++; ve++; }
        return json.substring(vs + 1, ve);
    }

    private TokenUsage parseUsage(String json) {
        String key = "\"usage\"";
        int idx = json.indexOf(key);
        if (idx < 0) return TokenUsage.empty();
        String sub = json.substring(idx);
        long pt = extractLong(sub, "prompt_tokens");
        long ct = extractLong(sub, "completion_tokens");
        long tt = extractLong(sub, "total_tokens");
        if (tt == 0) tt = pt + ct;
        return TokenUsage.of(pt, ct, tt);
    }

    private long extractLong(String json, String field) {
        String key = "\"" + field + "\":";
        int idx = json.indexOf(key);
        if (idx < 0) return 0;
        int vs = idx + key.length();
        while (vs < json.length() && json.charAt(vs) == ' ') vs++;
        int ve = vs;
        while (ve < json.length() && (Character.isDigit(json.charAt(ve)) || json.charAt(ve) == '-')) ve++;
        if (ve == vs) return 0;
        try { return Long.parseLong(json.substring(vs, ve)); } catch (NumberFormatException e) { return 0; }
    }

    private List<ToolCall> parseToolCalls(String json) {
        String key = "\"tool_calls\"";
        int idx = json.indexOf(key);
        if (idx < 0) return List.of();
        String sub = json.substring(idx);
        int arrStart = sub.indexOf('[');
        if (arrStart < 0) return List.of();
        int after = arrStart + 1;
        while (after < sub.length() && sub.charAt(after) == ' ') after++;
        if (after >= sub.length() || sub.charAt(after) == ']' || sub.startsWith("null", after)) return List.of();

        List<ToolCall> calls = new ArrayList<>();
        int depth = 0; int objStart = -1;
        for (int i = arrStart; i < sub.length(); i++) {
            char c = sub.charAt(i);
            if (c == '{') { if (depth == 1) objStart = i; depth++; }
            else if (c == '}') { depth--; if (depth == 1 && objStart >= 0) {
                String tcJson = sub.substring(objStart, i + 1);
                String id = extractField(tcJson, "id");
                String funcJson = extractObject(tcJson, "function");
                if (id != null && funcJson != null) {
                    String name = extractField(funcJson, "name");
                    String argsStr = extractField(funcJson, "arguments");
                    // G3-6: actually parse arguments instead of Map.of()
                    Map<String, Object> args = argsStr != null
                        ? parseJsonMap(argsStr) : Map.of();
                    if (name != null) calls.add(ToolCall.of(id, name, args));
                }
                objStart = -1;
            }} else if (c == ']' && depth == 0) break;
        }
        return calls;
    }

    private String extractObject(String json, String field) {
        String key = "\"" + field + "\":";
        int idx = json.indexOf(key);
        if (idx < 0) return null;
        int vs = idx + key.length();
        while (vs < json.length() && json.charAt(vs) == ' ') vs++;
        if (vs >= json.length() || json.charAt(vs) != '{') return null;
        int depth = 0;
        for (int i = vs; i < json.length(); i++) {
            if (json.charAt(i) == '{') depth++;
            else if (json.charAt(i) == '}') { depth--; if (depth == 0) return json.substring(vs, i + 1); }
        }
        return null;
    }

    private FinishReason parseFinishReason(String reason) {
        if (reason == null) return FinishReason.UNKNOWN;
        return switch (reason) {
            case "stop" -> FinishReason.STOP;
            case "length" -> FinishReason.LENGTH;
            case "tool_calls" -> FinishReason.TOOL_CALLS;
            case "content_filter" -> FinishReason.CONTENT_FILTER;
            default -> FinishReason.UNKNOWN;
        };
    }

    private String mapToJson(Map<String, Object> map) {
        if (map == null || map.isEmpty()) return "{}";
        StringBuilder sb = new StringBuilder("{");
        boolean first = true;
        for (var e : map.entrySet()) {
            if (!first) sb.append(",");
            first = false;
            sb.append("\"").append(esc(e.getKey())).append("\":").append(valueToJson(e.getValue()));
        }
        sb.append("}");
        return sb.toString();
    }

    private String valueToJson(Object v) {
        if (v == null) return "null";
        if (v instanceof String s) return "\"" + esc(s) + "\"";
        if (v instanceof Number || v instanceof Boolean) return v.toString();
        return "\"" + esc(v.toString()) + "\"";
    }

    private String esc(String s) {
        if (s == null) return "";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '"' -> sb.append("\\\""); case '\\' -> sb.append("\\\\");
                case '\n' -> sb.append("\\n"); case '\r' -> sb.append("\\r"); case '\t' -> sb.append("\\t");
                default -> { if (c < 0x20) sb.append(String.format("\\u%04x", (int) c)); else sb.append(c); }
            }
        }
        return sb.toString();
    }

    // G3-6: Jackson-based JSON parsing for tool call arguments
    private static final JsonFactory JSON_FACTORY = new JsonFactory();

    static Map<String, Object> parseJsonMap(String json) {
        Map<String, Object> map = new LinkedHashMap<>();
        try (JsonParser p = JSON_FACTORY.createParser(json)) {
            if (p.nextToken() != JsonToken.START_OBJECT) return map;
            return readObject(p);
        } catch (IOException e) {
            return map;
        }
    }

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
}
