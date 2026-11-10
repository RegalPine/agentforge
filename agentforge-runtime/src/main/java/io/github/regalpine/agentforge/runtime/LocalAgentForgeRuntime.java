package io.github.regalpine.agentforge.runtime;

import io.github.regalpine.agentforge.event.Execution;
import io.github.regalpine.agentforge.event.ExecutionId;

import java.util.Optional;

public final class LocalAgentForgeRuntime
    implements AgentForgeRuntime {

    private final ExecutionManager executionManager;

    public LocalAgentForgeRuntime(
        ExecutionManager executionManager
    ) {
        this.executionManager =
            executionManager;
    }

    @Override
    public ExecutionHandle submit(
        ExecutionRequest request
    ) {
        return executionManager.submit(
            request
        );
    }

    @Override
    public Optional<ExecutionHandle> get(
        ExecutionId executionId
    ) {
        return executionManager.get(
            executionId
        ).map(
            Execution::id
        ).map(
            SimpleExecutionHandle::new
        );
    }

    @Override
    public void pause(
        ExecutionId executionId
    ) {
        throw new UnsupportedOperationException(
            "Pause requires task-oriented runtime"
        );
    }

    @Override
    public void resume(
        ExecutionId executionId
    ) {
        throw new UnsupportedOperationException(
            "Resume requires task-oriented runtime"
        );
    }

    @Override
    public void cancel(
        ExecutionId executionId
    ) {
        throw new UnsupportedOperationException(
            "Cancel requires task-oriented runtime"
        );
    }
}
