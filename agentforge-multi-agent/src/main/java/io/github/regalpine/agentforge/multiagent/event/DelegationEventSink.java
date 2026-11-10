package io.github.regalpine.agentforge.multiagent.event;

import java.util.ArrayList;
import java.util.List;

/**
 * DelegationEventSink — AE-11 §38, §55.
 *
 * Sink for multi-agent delegation events.
 */
public interface DelegationEventSink {

    void emit(DelegationEvent event);

    static DelegationEventSink noop() {
        return event -> { };
    }

    /**
     * Collecting sink for testing.
     */
    static CollectingSink collecting() {
        return new CollectingSink();
    }

    final class CollectingSink implements DelegationEventSink {

        private final List<DelegationEvent> events
            = new ArrayList<>();

        @Override
        public synchronized void emit(DelegationEvent event) {
            events.add(event);
        }

        public synchronized List<DelegationEvent> events() {
            return List.copyOf(events);
        }

        public synchronized List<DelegationEvent> eventsOfType(
            DelegationEventType type
        ) {
            return events.stream()
                .filter(e -> e.type() == type)
                .toList();
        }

        public synchronized int size() {
            return events.size();
        }

        public synchronized void clear() {
            events.clear();
        }
    }
}
