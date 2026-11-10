package io.github.regalpine.agentforge.security;

import java.time.Instant;

public record SecurityContext(
    Identity identity,
    TenantContext tenant,
    Instant authenticatedAt
) {
}
