package io.github.regalpine.agentforge.provider.anthropic;

import java.net.URI;
import java.time.Duration;
import java.util.Objects;

/**
 * Configuration for the Anthropic API provider.
 */
public record AnthropicConfig(
    URI baseUrl,
    String apiKey,
    String anthropicVersion,
    Duration connectTimeout,
    Duration readTimeout,
    String defaultModel
) {

    public AnthropicConfig {
        Objects.requireNonNull(baseUrl, "baseUrl");
        Objects.requireNonNull(apiKey, "apiKey");
        if (apiKey.isBlank()) throw new IllegalArgumentException("apiKey cannot be blank");
        connectTimeout = connectTimeout == null ? Duration.ofSeconds(10) : connectTimeout;
        readTimeout = readTimeout == null ? Duration.ofSeconds(120) : readTimeout;
        anthropicVersion = anthropicVersion == null ? "2023-06-01" : anthropicVersion;
        defaultModel = defaultModel == null ? "claude-sonnet-4-20250514" : defaultModel;
    }

    public static AnthropicConfig of(String apiKey) {
        return new AnthropicConfig(
            URI.create("https://api.anthropic.com/v1"), apiKey,
            "2023-06-01", Duration.ofSeconds(10), Duration.ofSeconds(120), "claude-sonnet-4-20250514"
        );
    }
}
