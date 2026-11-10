package io.github.regalpine.agentforge.server.health;

public record HealthStatus(
    String status,
    String component,
    String details
) {

    public static HealthStatus healthy(
        String component
    ) {
        return new HealthStatus(
            "UP",
            component,
            null
        );
    }

    public static HealthStatus unhealthy(
        String component,
        String details
    ) {
        return new HealthStatus(
            "DOWN",
            component,
            details
        );
    }
}
