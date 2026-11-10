package io.github.regalpine.agentforge.server.execution;

import io.github.regalpine.agentforge.event.ExecutionEvent;
import io.github.regalpine.agentforge.event.ExecutionStatus;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * In-memory ExecutionStore for demo and unit-test use.
 */
public final class InMemoryExecutionStore implements ExecutionStore {

    private final Map<String, ExecutionRecord> records = new ConcurrentHashMap<>();
    private final Map<String, List<ExecutionEvent>> events = new ConcurrentHashMap<>();

    private static String key(String tenantId, String executionId) {
        return tenantId + ":" + executionId;
    }

    @Override
    public void create(ExecutionRecord record) {
        String k = key(record.tenantId(), record.executionId());
        ExecutionRecord existing = records.putIfAbsent(k, record);
        if (existing != null) {
            throw new IllegalStateException("Execution already exists: " + record.executionId());
        }
        events.put(k, new ArrayList<>());
    }

    @Override
    public Optional<ExecutionRecord> find(String tenantId, String executionId) {
        return Optional.ofNullable(records.get(key(tenantId, executionId)));
    }

    @Override
    public List<ExecutionRecord> list(String tenantId) {
        return records.values().stream()
            .filter(r -> tenantId.equals(r.tenantId()))
            .sorted(Comparator.comparing(ExecutionRecord::createdAt).reversed())
            .collect(Collectors.toList());
    }

    @Override
    public synchronized boolean updateStatus(String tenantId, String executionId,
                                              ExecutionStatus newStatus, long expectedVersion) {
        String k = key(tenantId, executionId);
        ExecutionRecord current = records.get(k);
        if (current == null) return false;
        if (current.version() != expectedVersion) return false;
        records.put(k, current.withStatus(newStatus, Instant.now()));
        return true;
    }

    @Override
    public synchronized void appendEvent(String tenantId, String executionId, ExecutionEvent event) {
        String k = key(tenantId, executionId);
        if (!records.containsKey(k)) {
            throw new IllegalStateException("Execution not found: " + executionId);
        }
        events.computeIfAbsent(k, x -> new ArrayList<>()).add(event);
    }

    @Override
    public List<ExecutionEvent> timeline(String tenantId, String executionId) {
        List<ExecutionEvent> list = events.get(key(tenantId, executionId));
        return list == null ? List.of() : List.copyOf(list);
    }
}
