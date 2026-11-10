package io.github.regalpine.agentforge.multiagent.team;

import java.util.Objects;
import java.util.UUID;

/**
 * TeamId — AF-ENHANCE-08 §19.
 *
 * Unique identity for an agent team.
 */
public record TeamId(
    String value
) {
    public TeamId {
        Objects.requireNonNull(
            value, "value"
        );
        if (value.isBlank()) {
            throw new IllegalArgumentException(
                "TeamId cannot be blank"
            );
        }
    }

    public static TeamId generate() {
        return new TeamId(
            UUID.randomUUID().toString()
        );
    }

    public static TeamId of(String value) {
        return new TeamId(value);
    }

    @Override
    public String toString() {
        return value;
    }
}
