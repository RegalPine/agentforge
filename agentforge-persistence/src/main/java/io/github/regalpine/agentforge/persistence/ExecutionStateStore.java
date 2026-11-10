package io.github.regalpine.agentforge.persistence;

import io.github.regalpine.agentforge.event.ExecutionId;
import io.github.regalpine.agentforge.event.ExecutionState;

import java.util.Optional;

public interface ExecutionStateStore {

    void create(
        ExecutionState state
    );

    Optional<ExecutionState> find(
        ExecutionId executionId
    );

    boolean compareAndSet(
        ExecutionId executionId,
        long expectedVersion,
        ExecutionState newState
    );
}
