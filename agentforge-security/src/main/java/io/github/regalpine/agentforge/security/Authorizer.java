package io.github.regalpine.agentforge.security;

public interface Authorizer {

    boolean authorize(
        AuthorizationRequest request
    );
}
