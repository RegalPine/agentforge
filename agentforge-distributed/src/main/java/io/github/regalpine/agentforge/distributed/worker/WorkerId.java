package io.github.regalpine.agentforge.distributed.worker;

import java.util.Objects;

public record WorkerId(
    String value
) {

    public WorkerId {
        Objects.requireNonNull(
            value,
            "value"
        );

        if (value.isBlank()) {
            throw new IllegalArgumentException(
                "WorkerId cannot be blank"
            );
        }
    }
}
