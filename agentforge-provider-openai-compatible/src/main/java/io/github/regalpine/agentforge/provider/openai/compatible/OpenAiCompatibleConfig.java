package io.github.regalpine.agentforge.provider.openai.compatible;

import java.net.URI;
import java.time.Duration;
import java.util.Objects;

/**
 * Configuration for OpenAI-Compatible API providers.
 * <p>
 * Supports vLLM, LM Studio, enterprise model gateways, and any
 * service that implements the OpenAI Chat Completions API protocol.
 */
public record OpenAiCompatibleConfig(
    URI baseUrl,
    String apiKey,
    String providerName,
    Duration connectTimeout,
    Duration readTimeout,
    String defaultModel
) {

    public OpenAiCompatibleConfig {
        Objects.requireNonNull(baseUrl, "baseUrl");
        Objects.requireNonNull(providerName, "providerName");
        apiKey = apiKey == null ? "" : apiKey;
        connectTimeout = connectTimeout == null ? Duration.ofSeconds(10) : connectTimeout;
        readTimeout = readTimeout == null ? Duration.ofSeconds(120) : readTimeout;
        defaultModel = defaultModel == null ? "default" : defaultModel;
    }

    /**
     * Create a configuration for a vLLM server.
     *
     * @param baseUrl the vLLM server URL
     * @return a new config
     */
    public static OpenAiCompatibleConfig vllm(String baseUrl) {
        return new OpenAiCompatibleConfig(
            URI.create(baseUrl), "", "vllm",
            Duration.ofSeconds(10), Duration.ofSeconds(120), "default"
        );
    }

    /**
     * Create a configuration for LM Studio.
     *
     * @return a new config pointing to default LM Studio local server
     */
    public static OpenAiCompatibleConfig lmStudio() {
        return new OpenAiCompatibleConfig(
            URI.create("http://localhost:1234/v1"), "", "lm-studio",
            Duration.ofSeconds(10), Duration.ofSeconds(120), "default"
        );
    }

    /**
     * Create a configuration for an enterprise model gateway.
     *
     * @param baseUrl the gateway URL
     * @param apiKey  the API key
     * @return a new config
     */
    public static OpenAiCompatibleConfig gateway(String baseUrl, String apiKey) {
        return new OpenAiCompatibleConfig(
            URI.create(baseUrl), apiKey, "enterprise-gateway",
            Duration.ofSeconds(10), Duration.ofSeconds(120), "default"
        );
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private URI baseUrl;
        private String apiKey = "";
        private String providerName = "openai-compatible";
        private Duration connectTimeout = Duration.ofSeconds(10);
        private Duration readTimeout = Duration.ofSeconds(120);
        private String defaultModel = "default";

        public Builder baseUrl(String baseUrl) { this.baseUrl = URI.create(baseUrl); return this; }
        public Builder apiKey(String apiKey) { this.apiKey = apiKey; return this; }
        public Builder providerName(String name) { this.providerName = name; return this; }
        public Builder connectTimeout(Duration t) { this.connectTimeout = t; return this; }
        public Builder readTimeout(Duration t) { this.readTimeout = t; return this; }
        public Builder defaultModel(String m) { this.defaultModel = m; return this; }
        public OpenAiCompatibleConfig build() { return new OpenAiCompatibleConfig(baseUrl, apiKey, providerName, connectTimeout, readTimeout, defaultModel); }
    }
}
