package io.github.regalpine.agentforge.provider.ollama;

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
 * NDJSON streaming publisher for Ollama /api/chat endpoint.
 * <p>
 * Ollama streams newline-delimited JSON objects, not SSE.
 */
class OllamaStreamingPublisher implements Flow.Publisher<ChatEvent> {

    private final OllamaConfig config;
    private final HttpClient httpClient;
    private final String modelId;
    private final ChatRequest request;

    OllamaStreamingPublisher(OllamaConfig config, HttpClient httpClient, String modelId, ChatRequest request) {
        this.config = config;
        this.httpClient = httpClient;
        this.modelId = modelId;
        this.request = request;
    }

    @Override
    public void subscribe(Flow.Subscriber<? super ChatEvent> subscriber) {
        SubmissionPublisher<ChatEvent> publisher = new SubmissionPublisher<>();
        publisher.subscribe(subscriber);
        publisher.submit(new ChatEvent.ChatStreamStarted(modelId, "ollama"));

        Thread thread = new Thread(() -> {
            try {
                OllamaChatModel helper = new OllamaChatModel(config, modelId);
                String requestBody = helper.buildRequestBody(request, true);

                HttpRequest httpRequest = HttpRequest.newBuilder()
                    .uri(URI.create(config.baseUrl() + "/api/chat"))
                    .header("Content-Type", "application/json")
                    .timeout(config.readTimeout())
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .build();

                HttpResponse<java.io.InputStream> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofInputStream());

                if (response.statusCode() >= 400) {
                    String errorBody = new String(response.body().readAllBytes());
                    publisher.submit(new ChatEvent.ChatStreamFailed("HTTP " + response.statusCode() + ": " + errorBody));
                    publisher.close();
                    return;
                }

                try (BufferedReader reader = new BufferedReader(new InputStreamReader(response.body()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        if (line.isEmpty()) continue;

                        // Each line is a JSON object
                        String content = extractStringField(line, "content");
                        boolean done = line.contains("\"done\":true");

                        if (content != null && !content.isEmpty()) {
                            publisher.submit(new ChatEvent.TextDelta(content));
                        }

                        if (done) {
                            long promptTokens = extractLongField(line, "prompt_eval_count");
                            long completionTokens = extractLongField(line, "eval_count");
                            if (promptTokens > 0 || completionTokens > 0) {
                                publisher.submit(new ChatEvent.UsageReceived(TokenUsage.of(promptTokens, completionTokens)));
                            }
                            publisher.submit(new ChatEvent.ChatStreamCompleted(FinishReason.STOP));
                            publisher.close();
                            return;
                        }
                    }
                    // If stream ended without done=true
                    publisher.submit(new ChatEvent.ChatStreamCompleted(FinishReason.STOP));
                }
                publisher.close();
            } catch (Exception e) {
                publisher.submit(new ChatEvent.ChatStreamFailed("Streaming error: " + e.getMessage(), e));
                publisher.close();
            }
        }, "ollama-stream-" + modelId);
        thread.setDaemon(true);
        thread.start();
    }

    private String extractStringField(String json, String field) {
        String key = "\"" + field + "\":";
        int idx = json.indexOf(key);
        if (idx < 0) return null;
        int vs = idx + key.length();
        while (vs < json.length() && json.charAt(vs) == ' ') vs++;
        if (vs >= json.length()) return null;
        if (json.charAt(vs) == '"') {
            int ve = vs + 1;
            while (ve < json.length() && json.charAt(ve) != '"') {
                if (json.charAt(ve) == '\\') ve++;
                ve++;
            }
            return json.substring(vs + 1, ve);
        }
        return null;
    }

    private long extractLongField(String json, String field) {
        String key = "\"" + field + "\":";
        int idx = json.indexOf(key);
        if (idx < 0) return 0;
        int vs = idx + key.length();
        while (vs < json.length() && json.charAt(vs) == ' ') vs++;
        if (vs >= json.length()) return 0;
        int ve = vs;
        while (ve < json.length() && Character.isDigit(json.charAt(ve))) ve++;
        if (ve == vs) return 0;
        try { return Long.parseLong(json.substring(vs, ve)); } catch (NumberFormatException e) { return 0; }
    }
}
