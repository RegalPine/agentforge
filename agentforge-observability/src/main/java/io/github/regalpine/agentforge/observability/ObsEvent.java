package io.github.regalpine.agentforge.observability;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;

/**
 * Unified observability event.
 * AF-ENHANCE-06 §3: four signal types
 * are separated — Event, Trace, Metric,
 * Audit — and dispatched through the
 * ObservabilityGateway.
 */
public record ObsEvent(
    SignalType signalType,
    String name,
    Instant timestamp,
    String traceId,
    String spanId,
    String executionId,
    String tenantId,
    Map<String, String> attributes,
    Object payload
) {

    public ObsEvent {
        Objects.requireNonNull(signalType);
        Objects.requireNonNull(name);
        timestamp = timestamp == null
            ? Instant.now() : timestamp;
        attributes = attributes == null
            ? Map.of() : Map.copyOf(attributes);
    }

    /**
     * Create an event signal.
     */
    public static ObsEvent event(
        String name,
        Map<String, String> attrs
    ) {
        return new ObsEvent(
            SignalType.EVENT, name,
            Instant.now(),
            null, null, null, null,
            attrs, null
        );
    }

    /**
     * Create a trace signal.
     */
    public static ObsEvent trace(
        String name,
        String traceId,
        String spanId,
        Map<String, String> attrs
    ) {
        return new ObsEvent(
            SignalType.TRACE, name,
            Instant.now(),
            traceId, spanId,
            null, null,
            attrs, null
        );
    }

    /**
     * Create a metric signal.
     */
    public static ObsEvent metric(
        String name,
        double value,
        Map<String, String> attrs
    ) {
        return new ObsEvent(
            SignalType.METRIC, name,
            Instant.now(),
            null, null, null, null,
            attrs, value
        );
    }

    /**
     * Create an audit signal.
     */
    public static ObsEvent audit(
        String name,
        String tenantId,
        Map<String, String> attrs
    ) {
        return new ObsEvent(
            SignalType.AUDIT, name,
            Instant.now(),
            null, null, null,
            tenantId,
            attrs, null
        );
    }
}
