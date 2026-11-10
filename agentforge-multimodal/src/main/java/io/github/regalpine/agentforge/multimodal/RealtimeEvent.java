package io.github.regalpine.agentforge.multimodal;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;

/**
 * Realtime event.
 * AF-ENHANCE-13 §34: RealtimeEvent.
 */
public record RealtimeEvent(
    String eventId,
    String type,
    long sequence,
    Object payload,
    Instant timestamp,
    Map<String, Object> metadata
) {

    public RealtimeEvent {
        Objects.requireNonNull(
            eventId, "eventId"
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
    public static RealtimeEvent of(
        String type,
        long sequence,
        Object payload
    ) {
        return new RealtimeEvent(
            "re-" + System.nanoTime(),
            type, sequence,
            payload, Instant.now(),
            Map.of()
        );
    }
}
