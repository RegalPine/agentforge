package io.github.regalpine.agentforge.memory;

import java.util.Objects;

/**
 * Memory event.
 * AE-08 §67: Memory Events.
 *
 * <p>Reuses the event pattern from
 * AE-04 Event Stream.
 */
public record MemoryEvent(
    MemoryEventType type,
    String subjectId,
    long timestamp
) {

    public MemoryEvent {
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(subjectId, "subjectId");

        if (timestamp <= 0) {
            timestamp = System.currentTimeMillis();
        }
    }
}
