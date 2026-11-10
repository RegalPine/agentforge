package io.github.regalpine.agentforge.provider.openai;

import io.github.regalpine.agentforge.model.*;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Objects;

/**
 * OpenAI EmbeddingModel implementation.
 */
public class OpenAiProviderEmbeddingModel implements EmbeddingModel {

    private final OpenAiProviderConfig config;
    private final HttpClient httpClient;
    private final String modelId;

    public OpenAiProviderEmbeddingModel(OpenAiProviderConfig config, String modelId) {
        this.config = Objects.requireNonNull(config, "config");
        this.modelId = Objects.requireNonNull(modelId, "modelId");
        this.httpClient = HttpClient.newBuilder()
            .connectTimeout(config.connectTimeout())
            .build();
    }

    @Override
    public EmbeddingResponse embed(EmbeddingRequest request) {
        Objects.requireNonNull(request, "request");
        String requestBody = OpenAiJson.buildEmbeddingRequestBody(modelId, request);

        HttpRequest httpRequest = HttpRequest.newBuilder()
            .uri(URI.create(config.baseUrl() + "/embeddings"))
            .header("Content-Type", "application/json")
            .header("Authorization", "Bearer " + config.apiKey())
            .timeout(config.readTimeout())
            .POST(HttpRequest.BodyPublishers.ofString(requestBody))
            .build();

        try {
            HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 400) {
                throw new ModelException("HTTP " + response.statusCode() + ": " + response.body(), modelId, "openai");
            }
            return OpenAiJson.parseEmbeddingResponse(response.body());
        } catch (ModelException e) {
            throw e;
        } catch (java.io.IOException e) {
            throw new ModelException("Network error communicating with OpenAI", modelId, "openai", e);
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
}
