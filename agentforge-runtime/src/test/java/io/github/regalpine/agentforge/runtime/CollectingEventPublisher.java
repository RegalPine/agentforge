package io.github.regalpine.agentforge.runtime;

import io.github.regalpine.agentforge.event.ExecutionEvent;
import io.github.regalpine.agentforge.event.EventPublisher;

import java.util.ArrayList;
import java.util.List;

final class CollectingEventPublisher
    implements EventPublisher {

    private final List<ExecutionEvent> events =
        new ArrayList<>();

    @Override
    public synchronized void publish(
        ExecutionEvent event
    ) {
        events.add(event);
    }

    synchronized List<ExecutionEvent> events() {
        return List.copyOf(events);
    }
}
