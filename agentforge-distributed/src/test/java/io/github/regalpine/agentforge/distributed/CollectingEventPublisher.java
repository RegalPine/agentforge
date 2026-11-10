package io.github.regalpine.agentforge.distributed;

import io.github.regalpine.agentforge.event.EventPublisher;
import io.github.regalpine.agentforge.event.ExecutionEvent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class CollectingEventPublisher
    implements EventPublisher {

    private final List<ExecutionEvent> events =
        new ArrayList<>();

    @Override
    public void publish(
        ExecutionEvent event
    ) {
        events.add(event);
    }

    public List<ExecutionEvent> events() {
        return Collections.unmodifiableList(
            events
        );
    }
}
