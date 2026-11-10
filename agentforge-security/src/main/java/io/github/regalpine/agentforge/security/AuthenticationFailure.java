package io.github.regalpine.agentforge.security;

public record AuthenticationFailure(
    String code,
    String message
) implements AuthenticationResult {
}
