package io.github.regalpine.agentforge.persistence;

import io.github.regalpine.agentforge.event.Execution;
import io.github.regalpine.agentforge.event.ExecutionId;

import java.util.Optional;

public interface ExecutionStore {

    void create(
        Execution execution
    );

    Optional<Execution> find(
        ExecutionId executionId
    );

    void update(
        Execution execution
    );
}
