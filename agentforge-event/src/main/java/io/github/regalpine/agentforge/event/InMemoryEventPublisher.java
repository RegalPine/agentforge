package io.github.regalpine.agentforge.event;

import java.util.ArrayList;
import java.util.List;

public final class InMemoryEventPublisher
    implements EventPublisher {

    private final List<ExecutionEvent> events =
        new ArrayList<>();

    @Override
    public synchronized void publish(
        ExecutionEvent event
    ) {
        events.add(event);
    }

    public synchronized List<ExecutionEvent> events() {
        return List.copyOf(events);
    }
}
