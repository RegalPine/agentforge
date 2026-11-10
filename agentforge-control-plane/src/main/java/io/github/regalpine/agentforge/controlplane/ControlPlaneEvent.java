package io.github.regalpine.agentforge.controlplane;

import java.time.Instant;
import java.util.Map;

/**
 * Control Plane audit event.
 * AF-ENHANCE-14 §67: Audit.
 */
public record ControlPlaneEvent(
    String eventId,
    String type,
    DeploymentId deploymentId,
    Instant timestamp,
    Map<String, String> data
) {

    public ControlPlaneEvent {
        if (eventId == null
            || eventId.isBlank()) {
            throw new IllegalArgumentException(
                "eventId required");
        }
        if (type == null
            || type.isBlank()) {
            throw new IllegalArgumentException(
                "type required");
        }
        data = data == null
            ? Map.of() : Map.copyOf(data);
        if (timestamp == null) {
            timestamp = Instant.now();
        }
    }

    /**
     * Create event.
     */
    public static ControlPlaneEvent of(
        String eventId,
        String type,
        DeploymentId deploymentId
    ) {
        return new ControlPlaneEvent(
            eventId, type,
            deploymentId,
            Instant.now(),
            Map.of()
        );
    }
}
