package io.github.regalpine.agentforge.observability;

public record DiagnosticError(
    String code,
    String category,
    String message,
    String component,
    String executionId,
    String nodeId,
    String invocationId,
    boolean retryable
) {
}
