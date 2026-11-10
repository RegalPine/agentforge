package io.github.regalpine.agentforge.security.iam;

import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * SPI for integrating with external IAM systems (OIDC/OAuth2).
 * <p>
 * Implementations validate external tokens and extract identity claims.
 * The adapter acts as the bridge between an external identity provider
 * and the AgentForge built-in user model.
 * <p>
 * Typical implementations:
 * <ul>
 *   <li>OIDC token validation against a configured issuer</li>
 *   <li>JWT signature + claims verification</li>
 *   <li>Delegation to enterprise IAM APIs</li>
 * </ul>
 */
public interface ExternalIamAdapter {

    /**
     * Validate an external token and extract identity claims.
     *
     * @param token the external token (e.g. JWT access token)
     * @return the extracted claims if the token is valid
     */
    Optional<ExternalIdentity> validate(String token);

    /**
     * Get the issuer identifier for this adapter.
     *
     * @return the issuer (e.g. OIDC issuer URL)
     */
    String issuer();

    /**
     * Represents an identity extracted from an external IAM.
     */
    record ExternalIdentity(
        String subject,
        String issuer,
        String email,
        String displayName,
        Set<String> externalRoles,
        Set<String> externalPermissions,
        Map<String, Object> claims
    ) {

        public ExternalIdentity {
            if (subject == null || subject.isBlank()) {
                throw new IllegalArgumentException(
                    "subject cannot be empty"
                );
            }
            if (issuer == null || issuer.isBlank()) {
                throw new IllegalArgumentException(
                    "issuer cannot be empty"
                );
            }
            externalRoles = externalRoles == null
                ? Set.of() : Set.copyOf(externalRoles);
            externalPermissions = externalPermissions == null
                ? Set.of() : Set.copyOf(externalPermissions);
            claims = claims == null
                ? Map.of() : Map.copyOf(claims);
        }
    }
}
