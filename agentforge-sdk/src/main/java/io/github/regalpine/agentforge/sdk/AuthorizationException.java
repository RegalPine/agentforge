package io.github.regalpine.agentforge.sdk;

/** Thrown when authorization fails. */
public class AuthorizationException extends AgentForgeException {
    public AuthorizationException(String message) {
        super("AUTHORIZATION_ERROR", message);
    }
}
