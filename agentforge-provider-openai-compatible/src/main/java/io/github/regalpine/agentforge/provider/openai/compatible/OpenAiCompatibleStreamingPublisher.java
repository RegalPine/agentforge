package io.github.regalpine.agentforge.provider.openai.compatible;

import io.github.regalpine.agentforge.model.*;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.concurrent.Flow;
import java.util.concurrent.SubmissionPublisher;

/**
 * SSE streaming publisher for OpenAI-compatible APIs.
 */
class OpenAiCompatibleStreamingPublisher implements Flow.Publisher<ChatEvent> {

    private final OpenAiCompatibleConfig config;
    private final HttpClient httpClient;
    private final String modelId;
    private final ChatRequest request;

    OpenAiCompatibleStreamingPublisher(OpenAiCompatibleConfig config, HttpClient httpClient, String modelId, ChatRequest request) {
        this.config = config;
        this.httpClient = httpClient;
        this.modelId = modelId;
        this.request = request;
    }

    @Override
    public void subscribe(Flow.Subscriber<? super ChatEvent> subscriber) {
        SubmissionPublisher<ChatEvent> publisher = new SubmissionPublisher<>();
        publisher.subscribe(subscriber);
        publisher.submit(new ChatEvent.ChatStreamStarted(modelId, config.providerName()));

        Thread thread = new Thread(() -> {
            try {
                OpenAiCompatibleChatModel helper = new OpenAiCompatibleChatModel(config, modelId);
                String requestBody = helper.buildRequestBody(request, true);

                HttpRequest.Builder builder = HttpRequest.newBuilder()
                    .uri(URI.create(config.baseUrl() + "/chat/completions"))
                    .header("Content-Type", "application/json")
                    .timeout(config.readTimeout())
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody));

                if (!config.apiKey().isEmpty()) {
                    builder.header("Authorization", "Bearer " + config.apiKey());
                }

                HttpResponse<java.io.InputStream> response = httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofInputStream());

                if (response.statusCode() >= 400) {
                    String errorBody = new String(response.body().readAllBytes());
                    publisher.submit(new ChatEvent.ChatStreamFailed("HTTP " + response.statusCode() + ": " + errorBody));
                    publisher.close();
                    return;
                }

                try (BufferedReader reader = new BufferedReader(new InputStreamReader(response.body()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        if (line.startsWith("data: ")) {
                            String data = line.substring(6).trim();
                            if (data.isEmpty()) continue;
                            if (data.equals("[DONE]")) {
                                publisher.submit(new ChatEvent.ChatStreamCompleted(FinishReason.STOP));
                                publisher.close();
                                return;
                            }

                            // Extract delta content
                            String content = extractDeltaContent(data);
                            if (content != null && !content.isEmpty()) {
                                publisher.submit(new ChatEvent.TextDelta(content));
                            }

                            // Check finish_reason
                            String finishReason = extractFinishReason(data);
                            if (finishReason != null && !finishReason.equals("null")) {
                                FinishReason fr = switch (finishReason) {
                                    case "stop" -> FinishReason.STOP;
                                    case "length" -> FinishReason.LENGTH;
                                    case "tool_calls" -> FinishReason.TOOL_CALLS;
                                    default -> FinishReason.UNKNOWN;
                                };
                                publisher.submit(new ChatEvent.ChatStreamCompleted(fr));
                                publisher.close();
                                return;
                            }
                        }
                    }
                    publisher.submit(new ChatEvent.ChatStreamCompleted(FinishReason.STOP));
                }
                publisher.close();
            } catch (Exception e) {
                publisher.submit(new ChatEvent.ChatStreamFailed("Streaming error: " + e.getMessage(), e));
                publisher.close();
            }
        }, "compat-stream-" + modelId);
        thread.setDaemon(true);
        thread.start();
    }

    private String extractDeltaContent(String json) {
        String deltaKey = "\"delta\"";
        int idx = json.indexOf(deltaKey);
        if (idx < 0) return null;
        String sub = json.substring(idx);
        int objStart = sub.indexOf('{');
        if (objStart < 0) return null;
        return extractField(sub.substring(objStart), "content");
    }

    private String extractFinishReason(String json) {
        String key = "\"finish_reason\":";
        int idx = json.indexOf(key);
        if (idx < 0) return null;
        int vs = idx + key.length();
        while (vs < json.length() && json.charAt(vs) == ' ') vs++;
        if (vs >= json.length()) return null;
        if (json.charAt(vs) == '"') {
            int ve = vs + 1;
            while (ve < json.length() && json.charAt(ve) != '"') { if (json.charAt(ve) == '\\') ve++; ve++; }
            return json.substring(vs + 1, ve);
        } else if (json.startsWith("null", vs)) return "null";
        return null;
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
}
