package io.github.regalpine.agentforge.runtime;

public record NodeFailed(
    String code,
    String message,
    boolean retryable
) implements NodeExecutionResult {
}
