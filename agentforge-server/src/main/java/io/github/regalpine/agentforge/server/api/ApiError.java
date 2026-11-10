package io.github.regalpine.agentforge.server.api;

public record ApiError(
    String code,
    String message,
    String requestId,
    boolean retryable
) {
}
