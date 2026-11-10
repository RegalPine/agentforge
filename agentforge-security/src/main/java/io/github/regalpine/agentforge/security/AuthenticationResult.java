package io.github.regalpine.agentforge.security;

public sealed interface AuthenticationResult
    permits AuthenticationSuccess, AuthenticationFailure {
}
