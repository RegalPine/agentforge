package io.github.regalpine.agentforge.agent;

import io.github.regalpine.agentforge.kernel.Reference;

import java.util.Map;
import java.util.Objects;

/**
 * Snapshot of an Agent's health.
 *
 * <p>Aggregates the overall status with
 * per-component health (loop, policy gate,
 * action port, memory, etc.).
 *
 * <p>Immutable. All maps are defensively
 * copied.
 */
public record AgentHealth(
    Reference reference,
    AgentHealthStatus status,
    Map<String, AgentComponentHealth> components
) {

    public AgentHealth {
        Objects.requireNonNull(reference, "reference");
        Objects.requireNonNull(status, "status");
        components = components == null
            ? Map.of()
            : Map.copyOf(components);
    }

    /**
     * Overall health status.
     */
    public enum AgentHealthStatus {
        UP,
        DEGRADED,
        DOWN
    }

    /**
     * Health of a single component.
     */
    public record AgentComponentHealth(
        String name,
        AgentHealthStatus status,
        String detail
    ) {
        public AgentComponentHealth {
            Objects.requireNonNull(name, "name");
            Objects.requireNonNull(status, "status");
        }

        public static AgentComponentHealth up(String name) {
            return new AgentComponentHealth(
                name, AgentHealthStatus.UP, null
            );
        }

        public static AgentComponentHealth down(
            String name, String detail
        ) {
            return new AgentComponentHealth(
                name, AgentHealthStatus.DOWN, detail
            );
        }
    }
}
