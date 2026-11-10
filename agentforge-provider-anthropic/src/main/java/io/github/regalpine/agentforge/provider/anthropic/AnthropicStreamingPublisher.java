package io.github.regalpine.agentforge.provider.anthropic;

import io.github.regalpine.agentforge.model.*;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.URI;
import java.util.concurrent.Flow;
import java.util.concurrent.SubmissionPublisher;

/**
 * SSE streaming publisher for Anthropic Messages API.
 */
class AnthropicStreamingPublisher implements Flow.Publisher<ChatEvent> {

    private final AnthropicConfig config;
    private final HttpClient httpClient;
    private final String modelId;
    private final ChatRequest request;

    AnthropicStreamingPublisher(AnthropicConfig config, HttpClient httpClient, String modelId, ChatRequest request) {
        this.config = config;
        this.httpClient = httpClient;
        this.modelId = modelId;
        this.request = request;
    }

    @Override
    public void subscribe(Flow.Subscriber<? super ChatEvent> subscriber) {
        SubmissionPublisher<ChatEvent> publisher = new SubmissionPublisher<>();
        publisher.subscribe(subscriber);
        publisher.submit(new ChatEvent.ChatStreamStarted(modelId, "anthropic"));

        Thread thread = new Thread(() -> {
            try {
                AnthropicChatModel helper = new AnthropicChatModel(config, modelId);
                String requestBody = helper.buildRequestBody(request, true);

                HttpRequest httpRequest = HttpRequest.newBuilder()
                    .uri(URI.create(config.baseUrl() + "/messages"))
                    .header("Content-Type", "application/json")
                    .header("x-api-key", config.apiKey())
                    .header("anthropic-version", config.anthropicVersion())
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
                        if (line.startsWith("event: ")) {
                            String eventType = line.substring(7).trim();
                            // Read the data line
                            String dataLine = reader.readLine();
                            if (dataLine != null && dataLine.startsWith("data: ")) {
                                String data = dataLine.substring(6).trim();
                                ChatEvent event = parseEvent(eventType, data);
                                if (event != null) {
                                    publisher.submit(event);
                                    if (event instanceof ChatEvent.ChatStreamCompleted || event instanceof ChatEvent.ChatStreamFailed) {
                                        publisher.close();
                                        return;
                                    }
                                }
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
        }, "anthropic-stream-" + modelId);
        thread.setDaemon(true);
        thread.start();
    }

    private ChatEvent parseEvent(String eventType, String data) {
        return switch (eventType) {
            case "content_block_delta" -> {
                // Extract delta text
                String text = extractField(data, "text");
                if (text != null) yield new ChatEvent.TextDelta(text);
                yield null;
            }
            case "message_delta" -> {
                String stopReason = extractField(data, "stop_reason");
                if (stopReason != null) {
                    FinishReason fr = switch (stopReason) {
                        case "end_turn" -> FinishReason.STOP;
                        case "max_tokens" -> FinishReason.LENGTH;
                        case "tool_use" -> FinishReason.TOOL_CALLS;
                        default -> FinishReason.UNKNOWN;
                    };
                    yield new ChatEvent.ChatStreamCompleted(fr);
                }
                yield null;
            }
            case "message_stop" -> new ChatEvent.ChatStreamCompleted(FinishReason.STOP);
            case "error" -> new ChatEvent.ChatStreamFailed("Stream error: " + data);
            default -> null;
        };
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
            while (ve < json.length() && json.charAt(ve) != '"') {
                if (json.charAt(ve) == '\\') ve++;
                ve++;
            }
            return json.substring(vs + 1, ve);
        }
        return null;
    }
}
