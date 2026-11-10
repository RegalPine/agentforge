package io.github.regalpine.agentforge.server.sse;

import io.github.regalpine.agentforge.event.ExecutionEvent;
import io.github.regalpine.agentforge.event.ExecutionId;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class SseEventBuffer {

    private final Map<
        ExecutionId,
        List<ExecutionEvent>
    > buffers = new ConcurrentHashMap<>();

    public void add(
        ExecutionEvent event
    ) {
        buffers
            .computeIfAbsent(
                event.executionId(),
                k -> Collections.synchronizedList(
                    new ArrayList<>()
                )
            )
            .add(event);
    }

    public List<ExecutionEvent> eventsAfter(
        ExecutionId executionId,
        long afterSequence
    ) {
        List<ExecutionEvent> events =
            buffers.get(executionId);

        if (events == null) {
            return List.of();
        }

        synchronized (events) {
            return events.stream()
                .filter(
                    e -> e.sequence() > afterSequence
                )
                .toList();
        }
    }

    public List<ExecutionEvent> allEvents(
        ExecutionId executionId
    ) {
        return eventsAfter(executionId, 0);
    }
}
