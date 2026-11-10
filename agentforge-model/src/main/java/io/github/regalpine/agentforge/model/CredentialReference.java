package io.github.regalpine.agentforge.model;

import java.util.Objects;

/**
 * Reference to a credential, used by providers and adapters to obtain
 * secrets without directly holding them.
 * <p>
 * Credential references are resolved at runtime through a {@link CredentialProvider}.
 * This ensures secrets are not embedded in provider configurations or agent definitions.
 */
public record CredentialReference(
    String credentialId,
    String type
) {

    public CredentialReference {
        Objects.requireNonNull(credentialId, "credentialId");
        Objects.requireNonNull(type, "type");
    }

    /**
     * Create a credential reference.
     *
     * @param credentialId the credential identifier
     * @param type         the credential type (e.g., "api-key", "bearer-token", "basic")
     * @return a new CredentialReference
     */
    public static CredentialReference of(String credentialId, String type) {
        return new CredentialReference(credentialId, type);
    }

    /**
     * Create an API key credential reference.
     *
     * @param credentialId the credential identifier
     * @return a new CredentialReference with type "api-key"
     */
    public static CredentialReference apiKey(String credentialId) {
        return new CredentialReference(credentialId, "api-key");
    }

    @Override
    public String toString() {
        return "CredentialReference[" + type + ":" + credentialId + "]";
    }
}
