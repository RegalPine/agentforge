package io.github.regalpine.agentforge.workspace;

import java.util.Objects;

/**
 * Artifact identity.
 * AF-ENHANCE-07 §15: Artifact Identity.
 */
public record ArtifactId(
    String value
) {
    public ArtifactId {
        Objects.requireNonNull(
            value, "value"
        );
        if (value.isBlank()) {
            throw new IllegalArgumentException(
                "ArtifactId cannot be blank"
            );
        }
    }
}
