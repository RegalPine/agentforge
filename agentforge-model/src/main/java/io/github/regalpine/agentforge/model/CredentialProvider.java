package io.github.regalpine.agentforge.model;

/**
 * SPI for resolving {@link CredentialReference} to {@link Credential}.
 * <p>
 * Implementations may source credentials from:
 * <ul>
 *   <li>Environment variables</li>
 *   <li>Secret management systems (Vault, AWS Secrets Manager, etc.)</li>
 *   <li>Configuration files (for development only)</li>
 * </ul>
 * <p>
 * Credential providers must not log or expose secret material.
 */
public interface CredentialProvider {

    /**
     * Resolve a credential reference to an actual credential.
     *
     * @param reference the credential reference
     * @return the resolved credential
     * @throws ModelException if the credential cannot be resolved
     */
    Credential resolve(CredentialReference reference);

    /**
     * Check if this provider can resolve the given reference.
     *
     * @param reference the credential reference
     * @return true if this provider can resolve the reference
     */
    boolean supports(CredentialReference reference);
}
