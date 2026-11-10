package io.github.regalpine.agentforge.observability.alert;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;

/**
 * Alert event.
 * AF-ENHANCE-06 §11: triggered when
 * SLO violations or anomalies are
 * detected.
 */
public record AlertEvent(
    String alertId,
    AlertSeverity severity,
    AlertType type,
    String message,
    Instant timestamp,
    Map<String, String> context
) {

    public AlertEvent {
        Objects.requireNonNull(alertId);
        Objects.requireNonNull(severity);
        Objects.requireNonNull(type);
        Objects.requireNonNull(message);
        timestamp = timestamp == null
            ? Instant.now() : timestamp;
        context = context == null
            ? Map.of() : Map.copyOf(context);
    }

    /**
     * Create a critical alert.
     */
    public static AlertEvent critical(
        String alertId,
        AlertType type,
        String message
    ) {
        return new AlertEvent(
            alertId,
            AlertSeverity.CRITICAL,
            type, message,
            Instant.now(), Map.of()
        );
    }

    /**
     * Create a warning alert.
     */
    public static AlertEvent warning(
        String alertId,
        AlertType type,
        String message
    ) {
        return new AlertEvent(
            alertId,
            AlertSeverity.WARNING,
            type, message,
            Instant.now(), Map.of()
        );
    }
}
