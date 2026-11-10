package io.github.regalpine.agentforge.workspace;

import java.util.Objects;

/**
 * Workspace identity.
 * AF-ENHANCE-07 §6: Workspace Identity.
 */
public record WorkspaceId(
    String value
) {
    public WorkspaceId {
        Objects.requireNonNull(
            value, "value"
        );
        if (value.isBlank()) {
            throw new IllegalArgumentException(
                "WorkspaceId cannot be blank"
            );
        }
    }
}
