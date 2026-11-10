package io.github.regalpine.agentforge.security;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * In-memory audit log implementation for development and testing.
 */
final class InMemoryAuditLog implements AuditLog {

    private final List<AuditEvent> events = Collections.synchronizedList(new ArrayList<>());

    @Override
    public void record(AuditEvent event) {
        events.add(event);
    }

    @Override
    public List<AuditEvent> recent(int limit) {
        synchronized (events) {
            int size = events.size();
            int from = Math.max(0, size - limit);
            List<AuditEvent> result = new ArrayList<>(events.subList(from, size));
            Collections.reverse(result);
            return result;
        }
    }
}
