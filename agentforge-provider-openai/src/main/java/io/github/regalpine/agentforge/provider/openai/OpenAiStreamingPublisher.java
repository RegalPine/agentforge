package io.github.regalpine.agentforge.provider.openai;

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
 * SSE streaming publisher for OpenAI Chat Completions API.
 * <p>
 * Parses Server-Sent Events (SSE) from the streaming response
 * and emits {@link ChatEvent} instances.
 */
class OpenAiStreamingPublisher implements Flow.Publisher<ChatEvent> {

    private final OpenAiProviderConfig config;
    private final HttpClient httpClient;
    private final String modelId;
    private final ChatRequest request;

    OpenAiStreamingPublisher(OpenAiProviderConfig config, HttpClient httpClient, String modelId, ChatRequest request) {
        this.config = config;
        this.httpClient = httpClient;
        this.modelId = modelId;
        this.request = request;
    }

    @Override
    public void subscribe(Flow.Subscriber<? super ChatEvent> subscriber) {
        SubmissionPublisher<ChatEvent> publisher = new SubmissionPublisher<>();
        publisher.subscribe(subscriber);

        // Emit stream started
        publisher.submit(new ChatEvent.ChatStreamStarted(modelId, "openai"));

        Thread thread = new Thread(() -> {
            try {
                String requestBody = OpenAiJson.buildChatRequestBody(modelId, request, true);
                HttpRequest httpRequest = HttpRequest.newBuilder()
                    .uri(URI.create(config.baseUrl() + "/chat/completions"))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + config.apiKey())
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
                        if (line.startsWith("data: ")) {
                            String data = line.substring(6).trim();
                            if (data.isEmpty()) continue;

                            ChatEvent event = OpenAiJson.parseStreamEvent(data);
                            if (event != null) {
                                publisher.submit(event);
                                if (event instanceof ChatEvent.ChatStreamCompleted || event instanceof ChatEvent.ChatStreamFailed) {
                                    publisher.close();
                                    return;
                                }
                            }
                        }
                    }
                    // If we get here without a completed event, emit one
                    publisher.submit(new ChatEvent.ChatStreamCompleted(FinishReason.STOP));
                }
                publisher.close();

            } catch (Exception e) {
                publisher.submit(new ChatEvent.ChatStreamFailed("Streaming error: " + e.getMessage(), e));
                publisher.close();
            }
        }, "openai-stream-" + modelId);
        thread.setDaemon(true);
        thread.start();
    }
}
