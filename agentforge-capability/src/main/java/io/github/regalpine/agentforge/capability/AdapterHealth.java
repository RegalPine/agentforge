package io.github.regalpine.agentforge.capability;

import java.time.Instant;
import java.util.Map;

/**
 * Adapter health status.
 * AF-ENHANCE-02 §4: adapters report
 * their health so the registry can
 * route invocations away from
 * unhealthy adapters.
 */
public record AdapterHealth(
    AdapterHealthStatus status,
    Instant checkedAt,
    String message,
    Map<String, Object> details
) {

    public AdapterHealth {
        status =
            status == null
                ? AdapterHealthStatus.UNKNOWN
                : status;

        checkedAt =
            checkedAt == null
                ? Instant.now()
                : checkedAt;

        details =
            details == null
                ? Map.of()
                : Map.copyOf(details);
    }

    public static AdapterHealth available() {
        return new AdapterHealth(
            AdapterHealthStatus.AVAILABLE,
            Instant.now(),
            null,
            Map.of()
        );
    }

    public static AdapterHealth unavailable(
        String reason
    ) {
        return new AdapterHealth(
            AdapterHealthStatus.UNAVAILABLE,
            Instant.now(),
            reason,
            Map.of()
        );
    }

    public static AdapterHealth unknown() {
        return new AdapterHealth(
            AdapterHealthStatus.UNKNOWN,
            Instant.now(),
            null,
            Map.of()
        );
    }
}
