package io.github.regalpine.agentforge.provider.llm.openai;

import java.net.URI;
import java.time.Duration;
import java.util.Objects;

/**
 * Configuration for OpenAI-compatible API provider.
 */
public record OpenAiConfig(
    URI baseUrl,
    String apiKey,
    String organizationId,
    Duration connectTimeout,
    Duration readTimeout,
    String defaultModel
) {

    public OpenAiConfig {
        Objects.requireNonNull(baseUrl, "baseUrl");
        Objects.requireNonNull(apiKey, "apiKey");

        if (apiKey.isBlank()) {
            throw new IllegalArgumentException("apiKey cannot be blank");
        }

        connectTimeout = connectTimeout == null ? Duration.ofSeconds(10) : connectTimeout;
        readTimeout = readTimeout == null ? Duration.ofSeconds(60) : readTimeout;
        defaultModel = defaultModel == null ? "gpt-4" : defaultModel;
    }

    /**
     * Create a configuration for OpenAI API.
     *
     * @param apiKey the API key
     * @return a new OpenAiConfig
     */
    public static OpenAiConfig openAi(String apiKey) {
        return new OpenAiConfig(
            URI.create("https://api.openai.com/v1"),
            apiKey,
            null,
            Duration.ofSeconds(10),
            Duration.ofSeconds(60),
            "gpt-4"
        );
    }

    /**
     * Create a configuration for a custom OpenAI-compatible endpoint.
     *
     * @param baseUrl the base URL
     * @param apiKey  the API key
     * @return a new OpenAiConfig
     */
    public static OpenAiConfig compatible(String baseUrl, String apiKey) {
        return new OpenAiConfig(
            URI.create(baseUrl),
            apiKey,
            null,
            Duration.ofSeconds(10),
            Duration.ofSeconds(60),
            "default"
        );
    }

    /**
     * Create a builder for OpenAiConfig.
     *
     * @return a new builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for OpenAiConfig.
     */
    public static class Builder {
        private URI baseUrl = URI.create("https://api.openai.com/v1");
        private String apiKey;
        private String organizationId;
        private Duration connectTimeout = Duration.ofSeconds(10);
        private Duration readTimeout = Duration.ofSeconds(60);
        private String defaultModel = "gpt-4";

        public Builder baseUrl(String baseUrl) {
            this.baseUrl = URI.create(baseUrl);
            return this;
        }

        public Builder apiKey(String apiKey) {
            this.apiKey = apiKey;
            return this;
        }

        public Builder organizationId(String organizationId) {
            this.organizationId = organizationId;
            return this;
        }

        public Builder connectTimeout(Duration connectTimeout) {
            this.connectTimeout = connectTimeout;
            return this;
        }

        public Builder readTimeout(Duration readTimeout) {
            this.readTimeout = readTimeout;
            return this;
        }

        public Builder defaultModel(String defaultModel) {
            this.defaultModel = defaultModel;
            return this;
        }

        public OpenAiConfig build() {
            return new OpenAiConfig(baseUrl, apiKey, organizationId, connectTimeout, readTimeout, defaultModel);
        }
    }
}
