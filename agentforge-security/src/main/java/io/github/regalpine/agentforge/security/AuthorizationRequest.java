package io.github.regalpine.agentforge.security;

public record AuthorizationRequest(
    Identity identity,
    String tenantId,
    String resourceType,
    String resourceId,
    String action
) {
}
