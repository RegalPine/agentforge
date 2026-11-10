package io.github.regalpine.agentforge.memory;

/**
 * Memory event sink.
 * AE-08 §66-67: Memory Observability.
 *
 * <p>Allows external systems to receive
 * memory lifecycle events for auditing,
 * metrics or debugging.
 */
public interface MemoryEventSink {

    /**
     * Emit a memory event.
     */
    void emit(MemoryEvent event);

    /**
     * No-op sink that discards all events.
     */
    static MemoryEventSink noop() {
        return event -> { };
    }
}
