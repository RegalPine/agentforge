package io.github.regalpine.agentforge.security.iam;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;

/**
 * Organization represents a top-level grouping for tenants.
 * <p>
 * An organization may own multiple tenants (e.g. dev, staging, production).
 * Organizations are optional — single-tenant deployments may omit them.
 */
public record Organization(
    String organizationId,
    String name,
    String displayName,
    Map<String, String> settings,
    Instant createdAt,
    Instant updatedAt
) {

    public Organization {
        Objects.requireNonNull(
            organizationId, "organizationId cannot be null"
        );
        Objects.requireNonNull(name, "name cannot be null");
        settings = settings == null
            ? Map.of() : Map.copyOf(settings);
        updatedAt = updatedAt == null
            ? Instant.now() : updatedAt;
        createdAt = createdAt == null
            ? Instant.now() : createdAt;
    }

    /**
     * Create a new organization with minimal configuration.
     */
    public static Organization create(
        String organizationId, String name
    ) {
        return new Organization(
            organizationId, name, name,
            Map.of(), Instant.now(), Instant.now()
        );
    }
}
