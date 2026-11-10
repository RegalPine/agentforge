package io.github.regalpine.agentforge.runtime;

import io.github.regalpine.agentforge.event.Execution;
import io.github.regalpine.agentforge.event.ExecutionState;
import io.github.regalpine.agentforge.event.ExecutionId;
import io.github.regalpine.agentforge.event.ExecutionEvent;
import io.github.regalpine.agentforge.event.EventPublisher;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * In-memory implementation of {@link CommitPort}.
 * Provides atomic state + event + activation + dispatch commit using ConcurrentHashMap.compute().
 */
public final class InMemoryCommitPort implements CommitPort {

    private final ConcurrentMap<ExecutionId, ExecutionState> states = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, List<ActivationChange>> activations = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, List<DispatchIntent>> dispatches = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, Map<String, Object>> outbox = new ConcurrentHashMap<>();
    private final EventPublisher eventPublisher;

    public InMemoryCommitPort(EventPublisher eventPublisher) {
        this.eventPublisher = eventPublisher;
    }

    @Override
    public void create(ExecutionState state) {
        if (states.putIfAbsent(state.executionId(), state) != null) {
            throw new IllegalStateException("Execution state already exists");
        }
    }

    public Optional<ExecutionState> get(ExecutionId executionId) {
        return Optional.ofNullable(states.get(executionId));
    }

    public List<ActivationChange> activations(ExecutionId id) {
        return activations.getOrDefault(id.value(), List.of());
    }

    public List<DispatchIntent> dispatches(ExecutionId id) {
        return dispatches.getOrDefault(id.value(), List.of());
    }

    public Map<String, Object> outbox(ExecutionId id) {
        return outbox.getOrDefault(id.value(), Map.of());
    }

    @Override
    public synchronized boolean commit(CommitCommand command) {
        ExecutionId id = command.newState().executionId();
        ExecutionState current = states.get(id);
        if (current == null || current.version() != command.expectedVersion()) {
            return false;
        }
        boolean replaced = states.replace(id, current, command.newState());
        if (!replaced) return false;

        // 同一原子工作单元：存储状态、事件、激活、派发意图和 Outbox
        eventPublisher.publish(command.event());
        for (ExecutionEvent extra : command.additionalEvents()) {
            eventPublisher.publish(extra);
        }
        if (!command.activationChanges().isEmpty()) {
            activations.merge(id.value(), List.copyOf(command.activationChanges()),
                (a, b) -> { var merged = new ArrayList<>(a); merged.addAll(b); return List.copyOf(merged); });
        }
        if (!command.dispatchIntents().isEmpty()) {
            dispatches.merge(id.value(), List.copyOf(command.dispatchIntents()),
                (a, b) -> { var merged = new ArrayList<>(a); merged.addAll(b); return List.copyOf(merged); });
        }
        if (!command.outboxEntries().isEmpty()) {
            outbox.merge(id.value(), Map.copyOf(command.outboxEntries()),
                (a, b) -> { var merged = new HashMap<>(a); merged.putAll(b); return Map.copyOf(merged); });
        }
        return true;
    }
}
