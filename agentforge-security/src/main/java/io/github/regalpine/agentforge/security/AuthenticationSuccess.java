package io.github.regalpine.agentforge.security;

public record AuthenticationSuccess(
    SecurityContext securityContext
) implements AuthenticationResult {
}
