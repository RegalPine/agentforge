package io.github.regalpine.agentforge.security.iam;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory {@link TenantStore} for development and testing.
 * <p>
 * Not suitable for production use — data does not survive restarts.
 */
public class InMemoryTenantStore implements TenantStore {

    private final Map<String, Tenant> tenants =
        new ConcurrentHashMap<>();

    private final Map<String, String> organizationIndex =
        new ConcurrentHashMap<>();

    @Override
    public void save(Tenant tenant) {
        tenants.put(tenant.tenantId(), tenant);
    }

    @Override
    public Optional<Tenant> findById(String tenantId) {
        return Optional.ofNullable(tenants.get(tenantId));
    }

    @Override
    public Optional<Tenant> findByName(String name) {
        return tenants.values().stream()
            .filter(t -> t.name().equals(name))
            .findFirst();
    }

    @Override
    public List<Tenant> findAll() {
        return List.copyOf(tenants.values());
    }

    @Override
    public List<Tenant> findByOrganization(
        String organizationId
    ) {
        return tenants.values().stream()
            .filter(t -> {
                Object orgId = t.settings().get("organizationId");
                return organizationId.equals(orgId);
            })
            .toList();
    }

    @Override
    public boolean delete(String tenantId) {
        return tenants.remove(tenantId) != null;
    }

    /**
     * Save a tenant and associate it with an organization.
     */
    public void save(Tenant tenant, String organizationId) {
        tenants.put(tenant.tenantId(), tenant);
        if (organizationId != null) {
            organizationIndex.put(
                tenant.tenantId(), organizationId
            );
        }
    }

    /**
     * Get the count of stored tenants.
     */
    public int size() {
        return tenants.size();
    }
}
