package io.github.regalpine.agentforge.security.iam;

import java.util.List;
import java.util.Optional;

/**
 * SPI for tenant persistence and lookup.
 * <p>
 * Implementations can back onto in-memory storage, relational databases,
 * or external directory services.
 */
public interface TenantStore {

    /**
     * Save or update a tenant.
     *
     * @param tenant the tenant to save
     */
    void save(Tenant tenant);

    /**
     * Find a tenant by its unique identifier.
     *
     * @param tenantId the tenant identifier
     * @return the tenant if found
     */
    Optional<Tenant> findById(String tenantId);

    /**
     * Find a tenant by its unique name.
     *
     * @param name the tenant name
     * @return the tenant if found
     */
    Optional<Tenant> findByName(String name);

    /**
     * List all tenants.
     *
     * @return all tenants
     */
    List<Tenant> findAll();

    /**
     * Find all tenants belonging to an organization.
     *
     * @param organizationId the organization identifier
     * @return matching tenants
     */
    List<Tenant> findByOrganization(String organizationId);

    /**
     * Delete a tenant by its identifier.
     *
     * @param tenantId the tenant identifier
     * @return true if the tenant existed and was removed
     */
    boolean delete(String tenantId);
}
