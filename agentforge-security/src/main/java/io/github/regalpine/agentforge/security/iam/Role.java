package io.github.regalpine.agentforge.security.iam;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Role represents a named set of permissions scoped to a tenant.
 * <p>
 * Permissions follow the {@code resource:action} convention:
 * <pre>
 *   graph:read
 *   graph:create
 *   execution:create
 *   capability:invoke
 *   admin:tenant
 * </pre>
 * <p>
 * A role can be tenant-scoped (most common) or global.
 * Global roles apply across all tenants.
 */
public record Role(
    String roleId,
    String name,
    String description,
    String tenantId,
    Set<String> permissions,
    Map<String, String> metadata,
    Instant createdAt,
    Instant updatedAt
) {

    public Role {
        Objects.requireNonNull(roleId, "roleId cannot be null");
        Objects.requireNonNull(name, "name cannot be null");
        permissions = permissions == null
            ? Set.of() : Set.copyOf(permissions);
        metadata = metadata == null
            ? Map.of() : Map.copyOf(metadata);
        updatedAt = updatedAt == null
            ? Instant.now() : updatedAt;
        createdAt = createdAt == null
            ? Instant.now() : createdAt;
    }

    /**
     * Create a tenant-scoped role.
     */
    public static Role tenantRole(
        String roleId,
        String name,
        String tenantId,
        Set<String> permissions
    ) {
        return new Role(
            roleId, name, null, tenantId,
            permissions, Map.of(),
            Instant.now(), Instant.now()
        );
    }

    /**
     * Create a global role (not scoped to any tenant).
     */
    public static Role global(
        String roleId,
        String name,
        Set<String> permissions
    ) {
        return new Role(
            roleId, name, null, null,
            permissions, Map.of(),
            Instant.now(), Instant.now()
        );
    }

    /**
     * Whether this role is global (applies to all tenants).
     */
    public boolean isGlobal() {
        return tenantId == null || tenantId.isBlank();
    }
}
