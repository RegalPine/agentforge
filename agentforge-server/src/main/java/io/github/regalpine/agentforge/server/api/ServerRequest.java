package io.github.regalpine.agentforge.server.api;

public record ServerRequest(
    String requestId,
    String method,
    String path,
    String tenantId,
    String authorizationHeader,
    String idempotencyKey,
    String body
) {
}
