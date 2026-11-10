package io.github.regalpine.agentforge.security.auth;

import io.github.regalpine.agentforge.security.*;

import java.time.Instant;
import java.util.Map;
import java.util.Set;

public final class StaticTokenAuthenticationProvider
    implements AuthenticationProvider {

    private final Map<String, Identity> tokens;

    public StaticTokenAuthenticationProvider(
        Map<String, Identity> tokens
    ) {
        this.tokens = Map.copyOf(tokens);
    }

    @Override
    public AuthenticationResult authenticate(
        AuthenticationRequest request
    ) {
        String header =
            request.authorizationHeader();

        if (header == null || header.isBlank()) {
            return new AuthenticationFailure(
                "AF-SEC-001",
                "Missing authorization header"
            );
        }

        String token = header;

        if (header.startsWith("Bearer ")) {
            token = header.substring(7);
        }

        Identity identity = tokens.get(token);

        if (identity == null) {
            return new AuthenticationFailure(
                "AF-SEC-002",
                "Invalid token"
            );
        }

        TenantContext tenant =
            new TenantContext(
                extractTenant(identity),
                identity.subjectId(),
                identity.permissions()
            );

        return new AuthenticationSuccess(
            new SecurityContext(
                identity,
                tenant,
                Instant.now()
            )
        );
    }

    private String extractTenant(
        Identity identity
    ) {
        Object claim =
            identity.claims().get("tenant");

        if (claim instanceof String t) {
            return t;
        }

        return "default";
    }
}
