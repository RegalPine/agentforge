package io.github.regalpine.agentforge.security.credential;

import java.util.Map;
import java.util.Optional;

public final class InMemoryCredentialProvider
    implements CredentialProvider {

    private final Map<String, ResolvedCredential> store;

    public InMemoryCredentialProvider(
        Map<String, ResolvedCredential> credentials
    ) {
        this.store = Map.copyOf(credentials);
    }

    @Override
    public Optional<ResolvedCredential> resolve(
        CredentialReference reference
    ) {
        return Optional.ofNullable(
            store.get(reference.referenceId())
        );
    }
}
