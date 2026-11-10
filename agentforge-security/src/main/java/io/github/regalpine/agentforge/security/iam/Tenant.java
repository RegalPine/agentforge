package io.github.regalpine.agentforge.security.iam;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;

/**
 * Tenant represents an isolated workspace or customer organization
 * within the AgentForge platform.
 * <p>
 * All resources (Graphs, Executions, etc.) are scoped to a tenant.
 * Tenant isolation is enforced at every layer of the security boundary.
 */
public record Tenant(
    String tenantId,
    String name,
    String displayName,
    TenantStatus status,
    Map<String, String> settings,
    Instant createdAt,
    Instant updatedAt
) {

    public Tenant {
        Objects.requireNonNull(tenantId, "tenantId cannot be null");
        Objects.requireNonNull(name, "name cannot be null");
        settings = settings == null ? Map.of() : Map.copyOf(settings);
        status = status == null ? TenantStatus.ACTIVE : status;
        updatedAt = updatedAt == null ? Instant.now() : updatedAt;
        createdAt = createdAt == null ? Instant.now() : createdAt;
    }

    /**
     * Create a new active tenant with minimal configuration.
     */
    public static Tenant create(String tenantId, String name) {
        return new Tenant(
            tenantId, name, name,
            TenantStatus.ACTIVE,
            Map.of(), Instant.now(), Instant.now()
        );
    }

    /**
     * Create a new tenant with display name.
     */
    public static Tenant create(
        String tenantId, String name, String displayName
    ) {
        return new Tenant(
            tenantId, name, displayName,
            TenantStatus.ACTIVE,
            Map.of(), Instant.now(), Instant.now()
        );
    }

    /**
     * Tenant lifecycle status.
     */
    public enum TenantStatus {
        ACTIVE,
        SUSPENDED,
        DEACTIVATED
    }
}
