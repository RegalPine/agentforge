package io.github.regalpine.agentforge.service;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;

/**
 * Agent service event.
 * AF-ENHANCE-11 §54-57: Event Streaming.
 *
 * <p>Service does not produce a
 * second event system. Events come
 * from Runtime Event (AF-ENHANCE-02).
 */
public record AgentEvent(
    String eventId,
    String type,
    String executionId,
    Object data,
    Instant timestamp,
    Map<String, Object> metadata
) {

    public AgentEvent {
        Objects.requireNonNull(
            eventId, "eventId"
        );
        Objects.requireNonNull(
            type, "type"
        );

        executionId = executionId == null
            ? "" : executionId;
        timestamp = timestamp == null
            ? Instant.now() : timestamp;
        metadata = metadata == null
            ? Map.of() : Map.copyOf(metadata);
    }

    /**
     * Create a simple event.
     */
    public static AgentEvent of(
        String type,
        String executionId,
        Object data
    ) {
        return new AgentEvent(
            "evt-" + System.nanoTime(),
            type, executionId,
            data, Instant.now(), Map.of()
        );
    }
}
