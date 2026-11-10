package io.github.regalpine.agentforge.runtime;

import io.github.regalpine.agentforge.event.Execution;
import io.github.regalpine.agentforge.event.ExecutionStatus;
import io.github.regalpine.agentforge.event.ExecutionId;

import java.util.Map;

/**
 * Execution result record.
 * AF-10 §27: ExecutionResult.
 *
 * <p>Represents the final outcome of a graph
 * execution, including outputs and optional
 * failure information.
 */
public record ExecutionResult(
    ExecutionId executionId,
    ExecutionStatus status,
    Map<String, Object> outputs,
    ExecutionFailure failure
) {

    public ExecutionResult {
        outputs =
            outputs == null
                ? Map.of()
                : Map.copyOf(outputs);
    }
}
