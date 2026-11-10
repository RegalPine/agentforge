package io.github.regalpine.agentforge.security;

import java.util.Set;

public record TenantContext(
    String tenantId,
    String subjectId,
    Set<String> permissions
) {

    public TenantContext {
        permissions =
            permissions == null
                ? Set.of()
                : Set.copyOf(permissions);
    }
}
