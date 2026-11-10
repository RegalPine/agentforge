package io.github.regalpine.agentforge.runtime;

import io.github.regalpine.agentforge.event.ExecutionState;
import io.github.regalpine.agentforge.event.ExecutionStatus;
import io.github.regalpine.agentforge.event.ExecutionId;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

/**
 * Simple in-memory ExecutionHandle implementation.
 * AF-10 §26: ExecutionHandle implementation.
 */
public final class SimpleExecutionHandle
    implements ExecutionHandle {

    private final ExecutionId executionId;
    private final ExecutionStatus status;
    private final ExecutionState state;
    private final CompletableFuture<ExecutionResult> completion;

    public SimpleExecutionHandle(
        ExecutionId executionId,
        ExecutionStatus status,
        ExecutionState state
    ) {
        this.executionId = executionId;
        this.status = status;
        this.state = state;
        this.completion = new CompletableFuture<>();
    }

    public SimpleExecutionHandle(
        ExecutionId executionId
    ) {
        this(executionId, ExecutionStatus.CREATED, null);
    }

    @Override
    public ExecutionId executionId() {
        return executionId;
    }

    @Override
    public ExecutionStatus status() {
        return status;
    }

    @Override
    public ExecutionState state() {
        return state;
    }

    @Override
    public CompletionStage<ExecutionResult> completion() {
        return completion;
    }

    public void complete(ExecutionResult result) {
        completion.complete(result);
    }

    public void completeExceptionally(Throwable ex) {
        completion.completeExceptionally(ex);
    }
}
