package io.github.regalpine.agentforge.security.audit;

import java.time.Duration;
import java.util.Objects;

/**
 * AuditRetention — AF-SEC-01 §57-59.
 *
 * Defines retention policy for audit events.
 * Different severity levels can have different
 * retention periods.
 *
 * Default: 90 days for all events.
 * CRITICAL events: 365 days.
 */
public record AuditRetention(

    Duration defaultRetention,

    Duration criticalRetention,

    Duration highRetention,

    Duration mediumRetention,

    Duration lowRetention,

    Duration infoRetention
) {

    public AuditRetention {
        defaultRetention = defaultRetention == null
            ? Duration.ofDays(90) : defaultRetention;
        criticalRetention = criticalRetention == null
            ? Duration.ofDays(365) : criticalRetention;
        highRetention = highRetention == null
            ? Duration.ofDays(180) : highRetention;
        mediumRetention = mediumRetention == null
            ? Duration.ofDays(90) : mediumRetention;
        lowRetention = lowRetention == null
            ? Duration.ofDays(30) : lowRetention;
        infoRetention = infoRetention == null
            ? Duration.ofDays(7) : infoRetention;
    }

    /**
     * Default retention policy.
     */
    public static AuditRetention defaults() {
        return new AuditRetention(
            null, null, null, null, null, null
        );
    }

    /**
     * Short retention for testing.
     */
    public static AuditRetention shortRetention() {
        return new AuditRetention(
            Duration.ofDays(7),
            Duration.ofDays(30),
            Duration.ofDays(14),
            Duration.ofDays(7),
            Duration.ofDays(3),
            Duration.ofDays(1)
        );
    }

    /**
     * Get the retention duration for a given severity.
     */
    public Duration retentionFor(
        io.github.regalpine.agentforge.security.AuditEvent.Severity severity
    ) {
        if (severity == null) return defaultRetention;
        return switch (severity) {
            case CRITICAL -> criticalRetention;
            case HIGH -> highRetention;
            case MEDIUM -> mediumRetention;
            case LOW -> lowRetention;
            case INFO -> infoRetention;
        };
    }
}
