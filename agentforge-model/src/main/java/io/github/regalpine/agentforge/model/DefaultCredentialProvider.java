package io.github.regalpine.agentforge.model;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Default in-memory credential provider for development and testing.
 * <p>
 * In production, use a proper secret management system instead.
 */
public class DefaultCredentialProvider implements CredentialProvider {

    private final Map<String, Credential> credentials = new ConcurrentHashMap<>();

    /**
     * Register a credential.
     *
     * @param credential the credential to store
     */
    public void register(Credential credential) {
        credentials.put(credential.credentialId(), credential);
    }

    /**
     * Register an API key credential.
     *
     * @param credentialId the credential identifier
     * @param apiKey       the API key value
     */
    public void registerApiKey(String credentialId, String apiKey) {
        register(Credential.apiKey(credentialId, apiKey));
    }

    @Override
    public Credential resolve(CredentialReference reference) {
        Credential credential = credentials.get(reference.credentialId());
        if (credential == null) {
            throw new ModelException(
                "Credential not found: " + reference.credentialId(),
                null,
                null
            );
        }
        if (credential.isExpired()) {
            throw new ModelException(
                "Credential expired: " + reference.credentialId(),
                null,
                null
            );
        }
        return credential;
    }

    @Override
    public boolean supports(CredentialReference reference) {
        return credentials.containsKey(reference.credentialId());
    }
}
