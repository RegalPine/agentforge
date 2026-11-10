package io.github.regalpine.agentforge.provider.ollama;

import io.github.regalpine.agentforge.model.*;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.*;

/**
 * Ollama EmbeddingModel implementation using /api/embed endpoint.
 */
public class OllamaEmbeddingModel implements EmbeddingModel {

    private final OllamaConfig config;
    private final HttpClient httpClient;
    private final String modelId;

    public OllamaEmbeddingModel(OllamaConfig config, String modelId) {
        this.config = Objects.requireNonNull(config, "config");
        this.modelId = Objects.requireNonNull(modelId, "modelId");
        this.httpClient = HttpClient.newBuilder()
            .connectTimeout(config.connectTimeout())
            .build();
    }

    @Override
    public EmbeddingResponse embed(EmbeddingRequest request) {
        Objects.requireNonNull(request, "request");

        List<EmbeddingVector> vectors = new ArrayList<>();
        List<String> texts = request.texts();

        for (int i = 0; i < texts.size(); i++) {
            String requestBody = "{\"model\":\"" + modelId + "\",\"prompt\":\"" + esc(texts.get(i)) + "\"}";

            HttpRequest httpRequest = HttpRequest.newBuilder()
                .uri(URI.create(config.baseUrl() + "/api/embeddings"))
                .header("Content-Type", "application/json")
                .timeout(config.readTimeout())
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();

            try {
                HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
                if (response.statusCode() >= 400) {
                    throw new ModelException("HTTP " + response.statusCode() + ": " + response.body(), modelId, "ollama");
                }
                List<Double> embedding = parseEmbedding(response.body());
                float[] floatValues = new float[embedding.size()];
                for (int j = 0; j < embedding.size(); j++) {
                    floatValues[j] = embedding.get(j).floatValue();
                }
                vectors.add(new EmbeddingVector(i, floatValues));
            } catch (ModelException e) {
                throw e;
            } catch (java.io.IOException e) {
                throw new ModelException("Network error communicating with Ollama", modelId, "ollama", e);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new ModelException("Request interrupted", modelId, "ollama", e);
            }
        }

        return new EmbeddingResponse(List.copyOf(vectors), TokenUsage.empty(), null, null);
    }

    @Override
    public String modelId() { return modelId; }

    @Override
    public String providerId() { return "ollama"; }

    private List<Double> parseEmbedding(String json) {
        String key = "\"embedding\":";
        int idx = json.indexOf(key);
        if (idx < 0) return List.of();

        int arrStart = json.indexOf('[', idx + key.length());
        if (arrStart < 0) return List.of();

        int arrEnd = json.indexOf(']', arrStart);
        if (arrEnd < 0) return List.of();

        String arrStr = json.substring(arrStart + 1, arrEnd);
        String[] parts = arrStr.split(",");
        List<Double> values = new ArrayList<>();
        for (String p : parts) {
            String trimmed = p.trim();
            if (!trimmed.isEmpty()) {
                try { values.add(Double.parseDouble(trimmed)); } catch (NumberFormatException e) { /* skip */ }
            }
        }
        return List.copyOf(values);
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
