package io.github.regalpine.agentforge.security.iam;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory {@link RoleStore} for development and testing.
 * <p>
 * Not suitable for production use — data does not survive restarts.
 */
public class InMemoryRoleStore implements RoleStore {

    private final Map<String, Role> rolesById =
        new ConcurrentHashMap<>();

    private final Set<RoleAssignment> assignments =
        ConcurrentHashMap.newKeySet();

    @Override
    public void saveRole(Role role) {
        rolesById.put(role.roleId(), role);
    }

    @Override
    public Optional<Role> findRoleById(String roleId) {
        return Optional.ofNullable(rolesById.get(roleId));
    }

    @Override
    public List<Role> findRolesByTenant(String tenantId) {
        return rolesById.values().stream()
            .filter(r -> tenantId.equals(r.tenantId()))
            .toList();
    }

    @Override
    public List<Role> findGlobalRoles() {
        return rolesById.values().stream()
            .filter(Role::isGlobal)
            .toList();
    }

    @Override
    public List<Role> findAllRoles() {
        return List.copyOf(rolesById.values());
    }

    @Override
    public boolean deleteRole(String roleId) {
        rolesById.remove(roleId);
        assignments.removeIf(
            a -> roleId.equals(a.roleId())
        );
        return true;
    }

    @Override
    public void assignRole(RoleAssignment assignment) {
        assignments.add(assignment);
    }

    @Override
    public boolean removeRoleAssignment(
        String userId, String roleId, String tenantId
    ) {
        return assignments.removeIf(a ->
            userId.equals(a.userId())
                && roleId.equals(a.roleId())
                && java.util.Objects.equals(
                    a.tenantId(), tenantId
                )
        );
    }

    @Override
    public Set<RoleAssignment> getAssignmentsForUserInTenant(
        String userId, String tenantId
    ) {
        Set<RoleAssignment> result = new HashSet<>();
        for (RoleAssignment a : assignments) {
            if (!userId.equals(a.userId())) {
                continue;
            }
            if (a.isGlobal()
                || tenantId.equals(a.tenantId())) {
                result.add(a);
            }
        }
        return Set.copyOf(result);
    }

    @Override
    public Set<RoleAssignment> getAllAssignmentsForUser(
        String userId
    ) {
        return assignments.stream()
            .filter(a -> userId.equals(a.userId()))
            .collect(
                java.util.stream.Collectors.toUnmodifiableSet()
            );
    }

    /**
     * Get the count of stored roles.
     */
    public int roleCount() {
        return rolesById.size();
    }

    /**
     * Get the count of role assignments.
     */
    public int assignmentCount() {
        return assignments.size();
    }
}
