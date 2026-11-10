package io.github.regalpine.agentforge.event;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 内存 EventStore 实现。
 */
public final class InMemoryEnvelopeEventStore
    implements EnvelopeEventStore {

    private final List<EventEnvelope> events =
        new CopyOnWriteArrayList<>();

    @Override
    public void append(
        EventEnvelope event
    ) {
        events.add(event);
    }

    @Override
    public List<EventEnvelope> read(
        String executionId
    ) {
        List<EventEnvelope> result =
            new ArrayList<>();
        for (EventEnvelope e : events) {
            if (executionId.equals(
                e.executionId()
            )) {
                result.add(e);
            }
        }
        result.sort(
            Comparator.comparingLong(
                EventEnvelope::sequence
            )
        );
        return List.copyOf(result);
    }

    @Override
    public List<EventEnvelope> readAfter(
        String executionId,
        long sequence
    ) {
        List<EventEnvelope> result =
            new ArrayList<>();
        for (EventEnvelope e : events) {
            if (executionId.equals(
                e.executionId()
            ) && e.sequence() > sequence) {
                result.add(e);
            }
        }
        result.sort(
            Comparator.comparingLong(
                EventEnvelope::sequence
            )
        );
        return List.copyOf(result);
    }

    @Override
    public List<EventEnvelope> readByTrace(
        String traceId
    ) {
        List<EventEnvelope> result =
            new ArrayList<>();
        for (EventEnvelope e : events) {
            if (traceId.equals(e.traceId())) {
                result.add(e);
            }
        }
        result.sort(
            Comparator.comparingLong(
                EventEnvelope::sequence
            )
        );
        return List.copyOf(result);
    }

    @Override
    public long count(
        String executionId
    ) {
        long c = 0;
        for (EventEnvelope e : events) {
            if (executionId.equals(
                e.executionId()
            )) {
                c++;
            }
        }
        return c;
    }

    public List<EventEnvelope> all() {
        return List.copyOf(events);
    }

    public void clear() {
        events.clear();
    }
}
