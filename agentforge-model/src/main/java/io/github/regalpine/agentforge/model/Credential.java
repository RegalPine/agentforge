package io.github.regalpine.agentforge.model;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;

/**
 * Resolved credential containing actual secret material.
 * <p>
 * Credentials are obtained from a {@link CredentialProvider} by resolving
 * a {@link CredentialReference}. They should be handled carefully and
 * never logged or persisted.
 */
public record Credential(
    String credentialId,
    String type,
    String secret,
    Map<String, String> properties,
    Instant expiresAt
) {

    public Credential {
        Objects.requireNonNull(credentialId, "credentialId");
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(secret, "secret");

        properties = properties == null ? Map.of() : Map.copyOf(properties);
    }

    /**
     * Create a simple API key credential.
     *
     * @param credentialId the credential identifier
     * @param apiKey       the API key value
     * @return a new Credential
     */
    public static Credential apiKey(String credentialId, String apiKey) {
        return new Credential(credentialId, "api-key", apiKey, null, null);
    }

    /**
     * Create a bearer token credential.
     *
     * @param credentialId the credential identifier
     * @param token        the bearer token value
     * @return a new Credential
     */
    public static Credential bearerToken(String credentialId, String token) {
        return new Credential(credentialId, "bearer-token", token, null, null);
    }

    /**
     * Check if this credential has expired.
     *
     * @return true if expired, false otherwise
     */
    public boolean isExpired() {
        return expiresAt != null && Instant.now().isAfter(expiresAt);
    }
}
