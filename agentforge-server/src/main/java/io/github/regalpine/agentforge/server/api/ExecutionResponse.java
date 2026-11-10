package io.github.regalpine.agentforge.server.api;

public record ExecutionResponse(
    String executionId,
    String graphId,
    String graphVersion,
    String status,
    String planFingerprint
) {
}
