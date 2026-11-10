package io.github.regalpine.agentforge.security.credential;

public record CredentialReference(
    String referenceId
) {

    public CredentialReference {
        if (referenceId == null || referenceId.isBlank()) {
            throw new IllegalArgumentException(
                "CredentialReference id cannot be blank"
            );
        }
    }
}
