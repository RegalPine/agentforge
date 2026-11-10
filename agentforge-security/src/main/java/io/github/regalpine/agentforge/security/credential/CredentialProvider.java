package io.github.regalpine.agentforge.security.credential;

import java.util.Optional;

public interface CredentialProvider {

    Optional<ResolvedCredential> resolve(
        CredentialReference reference
    );
}
