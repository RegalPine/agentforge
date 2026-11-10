package io.github.regalpine.agentforge.capability.http;

import java.util.Map;
import java.util.Objects;

/**
 * HTTP authentication configuration.
 * AF-ENHANCE-02 §8: supports
 * bearer token, API key and basic auth.
 */
public record HttpAuthConfig(
    HttpAuthType type,
    String credentialRef,
    Map<String, String> properties
) {

    public HttpAuthConfig {
        Objects.requireNonNull(type, "type");

        properties =
            properties == null
                ? Map.of()
                : Map.copyOf(properties);
    }

    public static HttpAuthConfig bearerToken(
        String credentialRef
    ) {
        return new HttpAuthConfig(
            HttpAuthType.BEARER_TOKEN,
            credentialRef,
            Map.of()
        );
    }

    public static HttpAuthConfig apiKey(
        String headerName,
        String credentialRef
    ) {
        return new HttpAuthConfig(
            HttpAuthType.API_KEY,
            credentialRef,
            Map.of("headerName", headerName)
        );
    }

    public static HttpAuthConfig basic(
        String credentialRef
    ) {
        return new HttpAuthConfig(
            HttpAuthType.BASIC,
            credentialRef,
            Map.of()
        );
    }

    public static HttpAuthConfig none() {
        return new HttpAuthConfig(
            HttpAuthType.NONE,
            null,
            Map.of()
        );
    }
}
