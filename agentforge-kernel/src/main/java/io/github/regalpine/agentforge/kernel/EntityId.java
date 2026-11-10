package io.github.regalpine.agentforge.kernel;

import java.util.Objects;

public record EntityId(String value) {

    public EntityId {
        Objects.requireNonNull(value, "value");

        if (value.isBlank()) {
            throw new IllegalArgumentException(
                "EntityId cannot be blank"
            );
        }

        if (!value.matches("[A-Za-z0-9._:-]+")) {
            throw new IllegalArgumentException(
                "Invalid EntityId: " + value
            );
        }
    }

    @Override
    public String toString() {
        return value;
    }
}
