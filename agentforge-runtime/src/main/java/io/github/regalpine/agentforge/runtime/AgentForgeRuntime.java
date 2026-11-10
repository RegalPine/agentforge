package io.github.regalpine.agentforge.runtime;

import io.github.regalpine.agentforge.event.ExecutionId;

import java.util.Optional;

public interface AgentForgeRuntime {

    ExecutionHandle submit(
        ExecutionRequest request
    );

    Optional<ExecutionHandle> get(
        ExecutionId executionId
    );

    void pause(
        ExecutionId executionId
    );

    void resume(
        ExecutionId executionId
    );

    void cancel(
        ExecutionId executionId
    );
}
