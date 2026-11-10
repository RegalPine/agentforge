package io.github.regalpine.agentforge.security.iam;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * User represents an identity subject in the built-in IAM system.
 * <p>
 * A user can be a human user or a service account (machine identity).
 * Users are assigned roles within specific tenants, and their effective
 * permissions are the union of all role permissions within that tenant.
 * <p>
 * Users may authenticate via:
 * <ul>
 *   <li>Built-in credentials (username + password hash)</li>
 *   <li>External IAM (OIDC/OAuth2 subject mapping)</li>
 * </ul>
 */
public record User(
    String userId,
    String username,
    String displayName,
    String email,
    UserType type,
    UserStatus status,
    String passwordHash,
    String externalSubject,
    String externalIssuer,
    Set<String> globalRoles,
    Map<String, Object> attributes,
    Instant createdAt,
    Instant updatedAt
) {

    public User {
        Objects.requireNonNull(userId, "userId cannot be null");
        Objects.requireNonNull(username, "username cannot be null");
        type = type == null ? UserType.HUMAN : type;
        status = status == null ? UserStatus.ACTIVE : status;
        globalRoles = globalRoles == null
            ? Set.of() : Set.copyOf(globalRoles);
        attributes = attributes == null
            ? Map.of() : Map.copyOf(attributes);
        updatedAt = updatedAt == null
            ? Instant.now() : updatedAt;
        createdAt = createdAt == null
            ? Instant.now() : createdAt;
    }

    /**
     * Create a human user with built-in credentials.
     */
    public static User human(
        String userId,
        String username,
        String passwordHash
    ) {
        return new User(
            userId, username, username, null,
            UserType.HUMAN, UserStatus.ACTIVE,
            passwordHash, null, null,
            Set.of(), Map.of(),
            Instant.now(), Instant.now()
        );
    }

    /**
     * Create a service account.
     */
    public static User serviceAccount(
        String userId, String username
    ) {
        return new User(
            userId, username, username, null,
            UserType.SERVICE, UserStatus.ACTIVE,
            null, null, null,
            Set.of(), Map.of(),
            Instant.now(), Instant.now()
        );
    }

    /**
     * Create a user linked to an external IAM identity.
     */
    public static User external(
        String userId,
        String username,
        String externalSubject,
        String externalIssuer
    ) {
        return new User(
            userId, username, username, null,
            UserType.HUMAN, UserStatus.ACTIVE,
            null, externalSubject, externalIssuer,
            Set.of(), Map.of(),
            Instant.now(), Instant.now()
        );
    }

    /**
     * Whether this user has built-in credentials.
     */
    public boolean hasBuiltInCredentials() {
        return passwordHash != null
            && !passwordHash.isBlank();
    }

    /**
     * Whether this user is linked to an external IAM.
     */
    public boolean hasExternalIdentity() {
        return externalSubject != null
            && !externalSubject.isBlank()
            && externalIssuer != null
            && !externalIssuer.isBlank();
    }

    /**
     * User type classification.
     */
    public enum UserType {
        HUMAN,
        SERVICE
    }

    /**
     * User lifecycle status.
     */
    public enum UserStatus {
        ACTIVE,
        DISABLED,
        LOCKED
    }
}
