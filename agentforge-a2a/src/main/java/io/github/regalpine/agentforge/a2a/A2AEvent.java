package io.github.regalpine.agentforge.a2a;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;

/**
 * A2A event.
 * AF-ENHANCE-10 §39-40: Streaming.
 */
public record A2AEvent(
    String eventId,
    String taskId,
    A2AEventType type,
    Object data,
    Instant timestamp,
    Map<String, Object> metadata
) {

    public A2AEvent {
        Objects.requireNonNull(
            eventId, "eventId"
        );
        Objects.requireNonNull(
            taskId, "taskId"
        );
        Objects.requireNonNull(
            type, "type"
        );

        timestamp = timestamp == null
            ? Instant.now() : timestamp;
        metadata = metadata == null
            ? Map.of() : Map.copyOf(metadata);
    }

    /**
     * Create a simple event.
     */
    public static A2AEvent of(
        String taskId,
        A2AEventType type
    ) {
        return new A2AEvent(
            "evt-" + System.nanoTime(),
            taskId, type, null,
            null, null
        );
    }

    /**
     * Create with data.
     */
    public static A2AEvent of(
        String taskId,
        A2AEventType type,
        Object data
    ) {
        return new A2AEvent(
            "evt-" + System.nanoTime(),
            taskId, type, data,
            null, null
        );
    }
}
