package io.github.regalpine.agentforge.runtime;

import io.github.regalpine.agentforge.event.Execution;
import io.github.regalpine.agentforge.event.ExecutionState;
import io.github.regalpine.agentforge.event.ExecutionStatus;
import io.github.regalpine.agentforge.event.ExecutionId;

import java.util.concurrent.CompletionStage;

/**
 * Execution handle interface.
 * AF-10 §26: ExecutionHandle.
 *
 * <p>Provides clients with access to execution
 * status, state, and async completion notification.
 */
public interface ExecutionHandle {

    ExecutionId executionId();

    ExecutionStatus status();

    ExecutionState state();

    CompletionStage<ExecutionResult> completion();
}
