package io.github.regalpine.agentforge.persistence.memory;

import io.github.regalpine.agentforge.persistence.ExecutionStateStore;
import io.github.regalpine.agentforge.event.ExecutionId;
import io.github.regalpine.agentforge.event.ExecutionState;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public final class InMemoryExecutionStateStore
    implements ExecutionStateStore {

    private final Map<
        ExecutionId,
        ExecutionState
    > states = new ConcurrentHashMap<>();

    @Override
    public void create(
        ExecutionState state
    ) {
        ExecutionState previous =
            states.putIfAbsent(
                state.executionId(),
                state
            );

        if (previous != null) {
            throw new IllegalStateException(
                "Execution state already exists: "
                    + state.executionId()
            );
        }
    }

    @Override
    public Optional<ExecutionState> find(
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

        if (current.version() != expectedVersion) {
            return false;
        }

        states.put(executionId, newState);
        return true;
    }
}
