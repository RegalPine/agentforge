package io.github.regalpine.agentforge.event;


import java.util.Objects;
import java.util.UUID;

public record ExecutionId(
    String value
) {

    public ExecutionId {
        Objects.requireNonNull(
            value,
            "value"
        );

        if (value.isBlank()) {
            throw new IllegalArgumentException(
                "ExecutionId cannot be blank"
            );
        }
    }

    public static ExecutionId generate() {
        return new ExecutionId(
            UUID.randomUUID().toString()
        );
    }

    @Override
    public String toString() {
        return value;
    }
}
