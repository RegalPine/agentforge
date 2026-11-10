package io.github.regalpine.agentforge.security;

public interface AuthenticationProvider {

    AuthenticationResult authenticate(
        AuthenticationRequest request
    );
}
