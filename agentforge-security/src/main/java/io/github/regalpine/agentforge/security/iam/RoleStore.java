package io.github.regalpine.agentforge.security.iam;

import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * SPI for role persistence and role assignment management.
 * <p>
 * Implementations can back onto in-memory storage, relational databases,
 * or external policy engines.
 */
public interface RoleStore {

    /**
     * Save or update a role definition.
     *
     * @param role the role to save
     */
    void saveRole(Role role);

    /**
     * Find a role by its unique identifier.
     *
     * @param roleId the role identifier
     * @return the role if found
     */
    Optional<Role> findRoleById(String roleId);

    /**
     * Find all roles scoped to a specific tenant.
     *
     * @param tenantId the tenant identifier
     * @return roles for that tenant
     */
    List<Role> findRolesByTenant(String tenantId);

    /**
     * Find all global roles.
     *
     * @return global roles
     */
    List<Role> findGlobalRoles();

    /**
     * List all roles.
     *
     * @return all roles
     */
    List<Role> findAllRoles();

    /**
     * Delete a role by its identifier.
     *
     * @param roleId the role identifier
     * @return true if the role existed and was removed
     */
    boolean deleteRole(String roleId);

    /**
     * Assign a role to a user within a tenant scope.
     *
     * @param assignment the role assignment
     */
    void assignRole(RoleAssignment assignment);

    /**
     * Remove a role assignment from a user.
     *
     * @param userId the user identifier
     * @param roleId the role identifier
     * @param tenantId the tenant scope (null for global)
     * @return true if the assignment existed and was removed
     */
    boolean removeRoleAssignment(
        String userId, String roleId, String tenantId
    );

    /**
     * Get all role assignments for a user within a tenant.
     * Includes both tenant-scoped and global assignments.
     *
     * @param userId the user identifier
     * @param tenantId the tenant identifier
     * @return role assignments applicable in this tenant
     */
    Set<RoleAssignment> getAssignmentsForUserInTenant(
        String userId, String tenantId
    );

    /**
     * Get all role assignments for a user across all tenants.
     *
     * @param userId the user identifier
     * @return all role assignments for this user
     */
    Set<RoleAssignment> getAllAssignmentsForUser(String userId);
}
