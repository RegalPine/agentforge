package io.github.regalpine.agentforge.server.health;

import io.github.regalpine.agentforge.graph.ir.ExecutionPlanRegistry;
import io.github.regalpine.agentforge.kernel.EntityId;
import io.github.regalpine.agentforge.kernel.Version;

/**
 * G4-2: Health check for the execution plan registry.
 *
 * <p>Verifies the registry is operational by performing
 * a probe lookup. In production mode with shared storage,
 * this ensures the registry dependency is available.
 */
public final class RegistryHealthCheck implements HealthCheck {

    private final ExecutionPlanRegistry registry;

    public RegistryHealthCheck(
        ExecutionPlanRegistry registry
    ) {
        this.registry = registry;
    }

    @Override
    public HealthStatus check() {
        try {
            // Probe: lookup a non-existent plan to verify
            // the registry is responsive
            registry.find(
                new EntityId("__health_probe__"),
                new Version("0.0.0")
            );
            return HealthStatus.healthy("registry");
        } catch (Exception e) {
            return HealthStatus.unhealthy(
                "registry",
                e.getMessage()
            );
        }
    }
}
