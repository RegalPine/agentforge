package io.github.regalpine.agentforge.persistence;

import io.github.regalpine.agentforge.event.ExecutionId;
import io.github.regalpine.agentforge.event.ExecutionState;
import io.github.regalpine.agentforge.event.StateManager;

import java.util.Optional;

public final class PersistentStateManager
    implements StateManager {

    private final ExecutionStateStore store;

    public PersistentStateManager(
        ExecutionStateStore store
    ) {
        this.store = store;
    }

    @Override
    public void create(
        ExecutionState state
    ) {
        store.create(state);
    }

    @Override
    public Optional<ExecutionState> get(
        ExecutionId executionId
    ) {
        return store.find(executionId);
    }

    @Override
    public boolean compareAndSet(
        ExecutionId executionId,
        long expectedVersion,
        ExecutionState newState
    ) {
        return store.compareAndSet(
            executionId,
            expectedVersion,
            newState
        );
    }
}
