package io.github.regalpine.agentforge.runtime;

import io.github.regalpine.agentforge.event.Execution;
import io.github.regalpine.agentforge.event.ExecutionState;
import io.github.regalpine.agentforge.event.StateManager;
import io.github.regalpine.agentforge.event.ExecutionId;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public final class InMemoryStateManager
    implements StateManager {

    private final ConcurrentMap<
        ExecutionId,
        ExecutionState
    > states =
        new ConcurrentHashMap<>();

    @Override
    public void create(
        ExecutionState state
    ) {
        if (states.putIfAbsent(
            state.executionId(),
            state
        ) != null) {

            throw new IllegalStateException(
                "Execution state already exists"
            );
        }
    }

    @Override
    public Optional<ExecutionState> get(
        ExecutionId executionId
    ) {
        return Optional.ofNullable(
            states.get(executionId)
        );
    }

    @Override
    public boolean compareAndSet(
        ExecutionId executionId,
        long expectedVersion,
        ExecutionState newState
    ) {

        ExecutionState current =
            states.get(executionId);

        if (current == null) {
            return false;
        }

        if (current.version()
            != expectedVersion) {

            return false;
        }

        return states.replace(
            executionId,
            current,
            newState
        );
    }
}
