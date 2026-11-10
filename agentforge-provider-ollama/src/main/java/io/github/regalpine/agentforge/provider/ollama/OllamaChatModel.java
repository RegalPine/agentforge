package io.github.regalpine.agentforge.provider.ollama;

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
 * Ollama ChatModel implementation using the Ollama /api/chat endpoint.
 * <p>
 * Supports chat, tool calling, and streaming.
 */
public class OllamaChatModel implements ChatModel {

    private final OllamaConfig config;
    private final HttpClient httpClient;
    private final String modelId;

    public OllamaChatModel(OllamaConfig config, String modelId) {
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
        return new OllamaStreamingPublisher(config, httpClient, modelId, request);
    }

    @Override
    public String modelId() { return modelId; }

    @Override
    public String providerId() { return "ollama"; }

    String buildRequestBody(ChatRequest request, boolean stream) {
        StringBuilder json = new StringBuilder();
        json.append("{");
        json.append("\"model\":\"").append(esc(modelId)).append("\"");
        json.append(",\"messages\":").append(buildMessages(request.messages()));

        if (stream) {
            json.append(",\"stream\":true");
        } else {
            json.append(",\"stream\":false");
        }

        if (request.tools() != null && !request.tools().isEmpty()) {
            json.append(",\"tools\":").append(buildTools(request.tools()));
        }

        if (request.temperature() != null) {
            json.append(",\"options\":{\"temperature\":").append(request.temperature());
            if (request.maxTokens() != null) {
                json.append(",\"num_predict\":").append(request.maxTokens());
            }
            json.append("}");
        } else if (request.maxTokens() != null) {
            json.append(",\"options\":{\"num_predict\":").append(request.maxTokens()).append("}");
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
                json.append("{\"role\":\"tool\",\"content\":\"").append(esc(t.content())).append("\"}");
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
            json.append("{\"type\":\"function\"");
            json.append(",\"function\":{\"name\":\"").append(esc(tool.id())).append("\"");
            json.append(",\"description\":\"").append(esc(tool.description())).append("\"");
            json.append(",\"parameters\":").append(mapToJson(tool.inputSchema()));
            json.append("}}");
        }
        json.append("]");
        return json.toString();
    }

    private String executeRequest(String requestBody) {
        HttpRequest httpRequest = HttpRequest.newBuilder()
            .uri(URI.create(config.baseUrl() + "/api/chat"))
            .header("Content-Type", "application/json")
            .timeout(config.readTimeout())
            .POST(HttpRequest.BodyPublishers.ofString(requestBody))
            .build();

        try {
            HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 400) {
                throw new ModelException("HTTP " + response.statusCode() + ": " + response.body(), modelId, "ollama");
            }
            return response.body();
        } catch (ModelException e) {
            throw e;
        } catch (java.io.IOException e) {
            throw new ModelException("Network error communicating with Ollama", modelId, "ollama", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ModelException("Request interrupted", modelId, "ollama", e);
        }
    }

    ChatResponse parseResponse(String responseBody) {
        String content = extractStringField(responseBody, "content");
        String model = extractStringField(responseBody, "model");
        String doneReason = extractStringField(responseBody, "done_reason");

        // Parse eval_count and prompt_eval_count for usage
        long promptTokens = extractLongField(responseBody, "prompt_eval_count");
        long completionTokens = extractLongField(responseBody, "eval_count");
        TokenUsage usage = TokenUsage.of(promptTokens, completionTokens);

        // Parse tool calls
        List<ToolCall> toolCalls = parseToolCalls(responseBody);

        FinishReason finishReason;
        if (!toolCalls.isEmpty()) {
            finishReason = FinishReason.TOOL_CALLS;
        } else if ("stop".equals(doneReason)) {
            finishReason = FinishReason.STOP;
        } else {
            finishReason = FinishReason.UNKNOWN;
        }

        return ChatResponse.builder()
            .content(content != null ? content : "")
            .toolCalls(toolCalls)
            .finishReason(finishReason)
            .usage(usage)
            .model(model != null ? model : modelId)
            .build();
    }

    private List<ToolCall> parseToolCalls(String json) {
        String key = "\"tool_calls\"";
        int idx = json.indexOf(key);
        if (idx < 0) return List.of();

        String sub = json.substring(idx);
        int arrStart = sub.indexOf('[');
        if (arrStart < 0) return List.of();

        int afterBracket = arrStart + 1;
        while (afterBracket < sub.length() && sub.charAt(afterBracket) == ' ') afterBracket++;
        if (afterBracket >= sub.length() || sub.charAt(afterBracket) == ']' || sub.startsWith("null", afterBracket)) return List.of();

        List<ToolCall> calls = new ArrayList<>();
        int depth = 0;
        int objStart = -1;
        for (int i = arrStart; i < sub.length(); i++) {
            char c = sub.charAt(i);
            if (c == '{') { if (depth == 1) objStart = i; depth++; }
            else if (c == '}') {
                depth--;
                if (depth == 1 && objStart >= 0) {
                    String tcJson = sub.substring(objStart, i + 1);
                    String funcObj = extractObjectField(tcJson, "function");
                    if (funcObj != null) {
                        String name = extractStringField(funcObj, "name");
                        String argsObj = extractObjectField(tcJson, "arguments");
                        Map<String, Object> args = argsObj != null ? parseJsonMap(argsObj) : Map.of();
                        if (name != null) {
                            calls.add(ToolCall.of("ollama-" + calls.size(), name, args));
                        }
                    }
                    objStart = -1;
                }
            } else if (c == ']' && depth == 0) break;
        }
        return calls;
    }

    // --- JSON helpers ---

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

    private String extractObjectField(String json, String field) {
        String key = "\"" + field + "\":";
        int keyIdx = json.indexOf(key);
        if (keyIdx < 0) return null;
        int vs = keyIdx + key.length();
        while (vs < json.length() && json.charAt(vs) == ' ') vs++;
        if (vs >= json.length() || json.charAt(vs) != '{') return null;
        int depth = 0;
        for (int i = vs; i < json.length(); i++) {
            if (json.charAt(i) == '{') depth++;
            else if (json.charAt(i) == '}') { depth--; if (depth == 0) return json.substring(vs, i + 1); }
        }
        return null;
    }

    // G3-6: Jackson-based parseJsonMap for nested structures
    private static final JsonFactory JSON_FACTORY = new JsonFactory();

    private Map<String, Object> parseJsonMap(String json) {
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

    private String mapToJson(Map<String, Object> map) {
        if (map == null || map.isEmpty()) return "{}";
        StringBuilder sb = new StringBuilder("{");
        boolean first = true;
        for (var e : map.entrySet()) {
            if (!first) sb.append(",");
            first = false;
            sb.append("\"").append(esc(e.getKey())).append("\":");
            Object v = e.getValue();
            if (v == null) sb.append("null");
            else if (v instanceof String s) sb.append("\"").append(esc(s)).append("\"");
            else if (v instanceof Number || v instanceof Boolean) sb.append(v);
            else sb.append("\"").append(esc(v.toString())).append("\"");
        }
        sb.append("}");
        return sb.toString();
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
