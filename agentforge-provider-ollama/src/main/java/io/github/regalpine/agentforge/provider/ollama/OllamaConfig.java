package io.github.regalpine.agentforge.provider.ollama;

import java.net.URI;
import java.time.Duration;
import java.util.Objects;

/**
 * Configuration for the Ollama provider.
 * <p>
 * Supports both local (default http://localhost:11434) and remote Ollama instances.
 */
public record OllamaConfig(
    URI baseUrl,
    Duration connectTimeout,
    Duration readTimeout,
    String defaultModel
) {

    public OllamaConfig {
        Objects.requireNonNull(baseUrl, "baseUrl");
        connectTimeout = connectTimeout == null ? Duration.ofSeconds(10) : connectTimeout;
        readTimeout = readTimeout == null ? Duration.ofSeconds(120) : readTimeout;
        defaultModel = defaultModel == null ? "llama3" : defaultModel;
    }

    /**
     * Create a configuration for a local Ollama instance.
     *
     * @return a new OllamaConfig pointing to localhost:11434
     */
    public static OllamaConfig local() {
        return new OllamaConfig(
            URI.create("http://localhost:11434"),
            Duration.ofSeconds(10),
            Duration.ofSeconds(120),
            "llama3"
        );
    }

    /**
     * Create a configuration for a remote Ollama instance.
     *
     * @param baseUrl the Ollama server URL
     * @return a new OllamaConfig
     */
    public static OllamaConfig remote(String baseUrl) {
        return new OllamaConfig(
            URI.create(baseUrl),
            Duration.ofSeconds(10),
            Duration.ofSeconds(120),
            "llama3"
        );
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private URI baseUrl = URI.create("http://localhost:11434");
        private Duration connectTimeout = Duration.ofSeconds(10);
        private Duration readTimeout = Duration.ofSeconds(120);
        private String defaultModel = "llama3";

        public Builder baseUrl(String baseUrl) { this.baseUrl = URI.create(baseUrl); return this; }
        public Builder connectTimeout(Duration t) { this.connectTimeout = t; return this; }
        public Builder readTimeout(Duration t) { this.readTimeout = t; return this; }
        public Builder defaultModel(String m) { this.defaultModel = m; return this; }
        public OllamaConfig build() { return new OllamaConfig(baseUrl, connectTimeout, readTimeout, defaultModel); }
    }
}
