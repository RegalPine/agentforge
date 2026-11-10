package io.github.regalpine.agentforge.memory;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * In-memory memory event sink.
 * AE-08 §66-67: Collects events for
 * testing and observability.
 */
public final class CollectingMemoryEventSink
    implements MemoryEventSink {

    private final List<MemoryEvent> events =
        new CopyOnWriteArrayList<>();

    @Override
    public void emit(MemoryEvent event) {
        events.add(event);
    }

    public List<MemoryEvent> events() {
        return List.copyOf(events);
    }

    public List<MemoryEvent> eventsOfType(
        MemoryEventType type
    ) {
        return events.stream()
            .filter(e -> e.type() == type)
            .toList();
    }

    public int eventCount() {
        return events.size();
    }

    public void clear() {
        events.clear();
    }
}
