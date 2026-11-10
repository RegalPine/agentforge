package io.github.regalpine.agentforge.rag.event;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Collecting knowledge event sink for testing.
 * AE-09 §66: Knowledge Events.
 */
public final class CollectingKnowledgeEventSink implements KnowledgeEventSink {

    private final List<KnowledgeEvent> events = Collections.synchronizedList(new ArrayList<>());

    @Override
    public void emit(KnowledgeEvent event) {
        events.add(event);
    }

    public List<KnowledgeEvent> events() {
        return List.copyOf(events);
    }

    public List<KnowledgeEvent> eventsOfType(KnowledgeEventType type) {
        return events.stream().filter(e -> e.type() == type).toList();
    }

    public int count() {
        return events.size();
    }

    public void clear() {
        events.clear();
    }
}
