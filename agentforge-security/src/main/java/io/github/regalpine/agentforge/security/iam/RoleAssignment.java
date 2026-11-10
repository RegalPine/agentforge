package io.github.regalpine.agentforge.security.iam;

import java.util.Objects;
import java.util.Set;

/**
 * Represents the assignment of a role to a user within a tenant scope.
 * <p>
 * If {@code tenantId} is null, the assignment is global and applies
 * to all tenants.
 */
public record RoleAssignment(
    String userId,
    String roleId,
    String tenantId
) {

    public RoleAssignment {
        Objects.requireNonNull(userId, "userId cannot be null");
        Objects.requireNonNull(roleId, "roleId cannot be null");
    }

    /**
     * Create a tenant-scoped role assignment.
     */
    public static RoleAssignment of(
        String userId,
        String roleId,
        String tenantId
    ) {
        return new RoleAssignment(userId, roleId, tenantId);
    }

    /**
     * Create a global role assignment (all tenants).
     */
    public static RoleAssignment global(
        String userId, String roleId
    ) {
        return new RoleAssignment(userId, roleId, null);
    }

    /**
     * Whether this assignment is global.
     */
    public boolean isGlobal() {
        return tenantId == null || tenantId.isBlank();
    }
}
