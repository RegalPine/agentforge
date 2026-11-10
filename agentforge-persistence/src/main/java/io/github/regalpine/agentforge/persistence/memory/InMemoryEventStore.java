package io.github.regalpine.agentforge.persistence.memory;

import io.github.regalpine.agentforge.persistence.EventStore;
import io.github.regalpine.agentforge.event.ExecutionEvent;
import io.github.regalpine.agentforge.event.ExecutionId;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public final class InMemoryEventStore
    implements EventStore {

    private final List<ExecutionEvent> events =
        new CopyOnWriteArrayList<>();

    @Override
    public void append(
        ExecutionEvent event
    ) {
        if (!events.isEmpty()) {
            ExecutionEvent last =
                events.get(events.size() - 1);

            if (last.executionId().equals(
                event.executionId()
            ) &&
                event.sequence() <=
                    last.sequence()
            ) {
                throw new IllegalStateException(
                    "Event sequence must be strictly increasing"
                );
            }
        }

        events.add(event);
    }

    @Override
    public List<ExecutionEvent> list(
        ExecutionId executionId
    ) {
        List<ExecutionEvent> result =
            new ArrayList<>();

        for (ExecutionEvent event : events) {
            if (event.executionId().equals(executionId)) {
                result.add(event);
            }
        }

        return List.copyOf(result);
    }

    @Override
    public List<ExecutionEvent> listAfter(
        ExecutionId executionId,
        long sequence
    ) {
        List<ExecutionEvent> result =
            new ArrayList<>();

        for (ExecutionEvent event : events) {
            if (event.executionId().equals(executionId) &&
                event.sequence() > sequence
            ) {
                result.add(event);
            }
        }

        return List.copyOf(result);
    }
}
