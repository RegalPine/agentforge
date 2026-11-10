package io.github.regalpine.agentforge.provider.openai;

import io.github.regalpine.agentforge.model.*;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Flow;

/**
 * OpenAI ChatModel implementation with streaming support.
 * <p>
 * Supports Chat Completion, Tool Calling, Structured Output, and Streaming
 * via the OpenAI Chat Completions API.
 */
public class OpenAiProviderChatModel implements ChatModel {

    private final OpenAiProviderConfig config;
    private final HttpClient httpClient;
    private final String modelId;

    public OpenAiProviderChatModel(OpenAiProviderConfig config, String modelId) {
        this.config = Objects.requireNonNull(config, "config");
        this.modelId = Objects.requireNonNull(modelId, "modelId");
        this.httpClient = HttpClient.newBuilder()
            .connectTimeout(config.connectTimeout())
            .build();
    }

    public OpenAiProviderChatModel(OpenAiProviderConfig config) {
        this(config, config.defaultModel());
    }

    @Override
    public ChatResponse chat(ChatRequest request) {
        Objects.requireNonNull(request, "request");
        String requestBody = OpenAiJson.buildChatRequestBody(modelId, request, false);
        String responseBody = executeRequest(requestBody);
        return OpenAiJson.parseChatResponse(responseBody);
    }

    @Override
    public CompletionStage<ChatResponse> chatAsync(ChatRequest request) {
        Objects.requireNonNull(request, "request");
        String requestBody = OpenAiJson.buildChatRequestBody(modelId, request, false);

        HttpRequest httpRequest = buildHttpRequest(requestBody);
        return httpClient.sendAsync(httpRequest, HttpResponse.BodyHandlers.ofString())
            .thenApply(response -> {
                handleErrors(response);
                return OpenAiJson.parseChatResponse(response.body());
            });
    }

    @Override
    public Flow.Publisher<ChatEvent> stream(ChatRequest request) {
        Objects.requireNonNull(request, "request");
        return new OpenAiStreamingPublisher(config, httpClient, modelId, request);
    }

    @Override
    public String modelId() {
        return modelId;
    }

    @Override
    public String providerId() {
        return "openai";
    }

    private String executeRequest(String requestBody) {
        HttpRequest httpRequest = buildHttpRequest(requestBody);
        try {
            HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
            handleErrors(response);
            return response.body();
        } catch (ModelException e) {
            throw e;
        } catch (java.io.IOException e) {
            throw new ModelException("Network error communicating with OpenAI", modelId, "openai", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ModelException("Request interrupted", modelId, "openai", e);
        }
    }

    private HttpRequest buildHttpRequest(String requestBody) {
        HttpRequest.Builder builder = HttpRequest.newBuilder()
            .uri(URI.create(config.baseUrl() + "/chat/completions"))
            .header("Content-Type", "application/json")
            .header("Authorization", "Bearer " + config.apiKey())
            .timeout(config.readTimeout())
            .POST(HttpRequest.BodyPublishers.ofString(requestBody));

        if (config.organizationId() != null && !config.organizationId().isBlank()) {
            builder.header("OpenAI-Organization", config.organizationId());
        }
        return builder.build();
    }

    private void handleErrors(HttpResponse<String> response) {
        if (response.statusCode() == 429) {
            Long retryAfter = response.headers().firstValue("Retry-After")
                .map(s -> { try { return Long.parseLong(s); } catch (NumberFormatException e) { return null; } })
                .orElse(null);
            throw new ModelRateLimitException("Rate limit exceeded", modelId, "openai", retryAfter);
        }
        if (response.statusCode() == 401) {
            throw new ModelException("Authentication failed: invalid API key", modelId, "openai");
        }
        if (response.statusCode() >= 400) {
            throw new ModelException("HTTP " + response.statusCode() + ": " + response.body(), modelId, "openai");
        }
    }
}
