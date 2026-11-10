package io.github.regalpine.agentforge.security.credential;

import java.util.Map;

public record ResolvedCredential(
    String referenceId,
    String type,
    Map<String, String> secrets
) {

    public ResolvedCredential {
        secrets =
            secrets == null
                ? Map.of()
                : Map.copyOf(secrets);
    }
}
