package io.github.regalpine.agentforge.observability;

public record CorrelationContext(
    String requestId,
    String traceId,
    String executionId,
    String nodeId,
    String invocationId,
    String taskId,
    String workerId,
    String providerId,
    String tenantId
) {
}
