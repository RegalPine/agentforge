package io.github.regalpine.agentforge.provider.openai;

import java.net.URI;
import java.time.Duration;
import java.util.Objects;

/**
 * Configuration for the OpenAI API provider.
 */
public record OpenAiProviderConfig(
    URI baseUrl,
    String apiKey,
    String organizationId,
    Duration connectTimeout,
    Duration readTimeout,
    String defaultModel
) {

    public OpenAiProviderConfig {
        Objects.requireNonNull(baseUrl, "baseUrl");
        Objects.requireNonNull(apiKey, "apiKey");
        if (apiKey.isBlank()) {
            throw new IllegalArgumentException("apiKey cannot be blank");
        }
        connectTimeout = connectTimeout == null ? Duration.ofSeconds(10) : connectTimeout;
        readTimeout = readTimeout == null ? Duration.ofSeconds(60) : readTimeout;
        defaultModel = defaultModel == null ? "gpt-4" : defaultModel;
    }

    public static OpenAiProviderConfig of(String apiKey) {
        return new OpenAiProviderConfig(
            URI.create("https://api.openai.com/v1"),
            apiKey, null,
            Duration.ofSeconds(10),
            Duration.ofSeconds(60),
            "gpt-4"
        );
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private URI baseUrl = URI.create("https://api.openai.com/v1");
        private String apiKey;
        private String organizationId;
        private Duration connectTimeout = Duration.ofSeconds(10);
        private Duration readTimeout = Duration.ofSeconds(60);
        private String defaultModel = "gpt-4";

        public Builder baseUrl(String baseUrl) { this.baseUrl = URI.create(baseUrl); return this; }
        public Builder apiKey(String apiKey) { this.apiKey = apiKey; return this; }
        public Builder organizationId(String organizationId) { this.organizationId = organizationId; return this; }
        public Builder connectTimeout(Duration connectTimeout) { this.connectTimeout = connectTimeout; return this; }
        public Builder readTimeout(Duration readTimeout) { this.readTimeout = readTimeout; return this; }
        public Builder defaultModel(String defaultModel) { this.defaultModel = defaultModel; return this; }
        public OpenAiProviderConfig build() { return new OpenAiProviderConfig(baseUrl, apiKey, organizationId, connectTimeout, readTimeout, defaultModel); }
    }
}
