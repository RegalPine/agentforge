package io.github.regalpine.agentforge.sdk;

import java.util.Map;
import java.util.Objects;

/**
 * Result of an Agent or Graph execution.
 * <p>
 * This is the authoritative final result of an execution.
 * Streaming events do not replace this result.
 */
public record ExecutionResult(
    String executionId,
    ExecutionStatus status,
    Map<String, Object> output,
    String errorMessage,
    Map<String, Object> metadata
) {

    public ExecutionResult {
        Objects.requireNonNull(executionId, "executionId cannot be null");
        Objects.requireNonNull(status, "status cannot be null");
        output = output == null ? Map.of() : Map.copyOf(output);
        metadata = metadata == null ? Map.of() : Map.copyOf(metadata);
    }

    /**
     * Get the primary output value.
     *
     * @return the output map (never null)
     */
    public Map<String, Object> output() {
        return output;
    }

    /**
     * Check if the execution completed successfully.
     */
    public boolean isSuccess() {
        return status == ExecutionStatus.COMPLETED;
    }

    public static ExecutionResult success(String executionId, Map<String, Object> output) {
        return new ExecutionResult(executionId, ExecutionStatus.COMPLETED, output, null, Map.of());
    }

    public static ExecutionResult failed(String executionId, String errorMessage) {
        return new ExecutionResult(executionId, ExecutionStatus.FAILED, Map.of(), errorMessage, Map.of());
    }
}
