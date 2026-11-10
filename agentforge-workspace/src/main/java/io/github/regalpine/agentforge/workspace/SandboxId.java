package io.github.regalpine.agentforge.workspace;

import java.util.Objects;

/**
 * Sandbox identity.
 * AF-ENHANCE-07 §31: Sandbox.
 */
public record SandboxId(
    String value
) {
    public SandboxId {
        Objects.requireNonNull(
            value, "value"
        );
        if (value.isBlank()) {
            throw new IllegalArgumentException(
                "SandboxId cannot be blank"
            );
        }
    }
}
