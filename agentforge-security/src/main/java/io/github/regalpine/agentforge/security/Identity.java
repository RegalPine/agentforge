package io.github.regalpine.agentforge.security;

import java.util.Map;
import java.util.Set;

public record Identity(
    String subjectId,
    String issuer,
    Set<String> roles,
    Set<String> permissions,
    Map<String, Object> claims
) {

    public Identity {
        roles =
            roles == null
                ? Set.of()
                : Set.copyOf(roles);

        permissions =
            permissions == null
                ? Set.of()
                : Set.copyOf(permissions);

        claims =
            claims == null
                ? Map.of()
                : Map.copyOf(claims);
    }
}
