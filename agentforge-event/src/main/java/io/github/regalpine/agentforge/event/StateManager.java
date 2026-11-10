package io.github.regalpine.agentforge.event;

import io.github.regalpine.agentforge.event.ExecutionId;

import java.util.Optional;

public interface StateManager {

    void create(ExecutionState state);

    Optional<ExecutionState> get(
        ExecutionId executionId
    );

    boolean compareAndSet(
        ExecutionId executionId,
        long expectedVersion,
        ExecutionState newState
    );
}
