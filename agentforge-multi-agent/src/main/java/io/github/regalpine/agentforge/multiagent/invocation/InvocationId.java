package io.github.regalpine.agentforge.multiagent.invocation;

import java.util.Objects;
import java.util.UUID;

/**
 * InvocationId — AF-ENHANCE-08 §5.
 *
 * Unique identity for an agent-to-agent
 * invocation. Each invocation creates
 * an independent execution relationship.
 */
public record InvocationId(
    String value
) {
    public InvocationId {
        Objects.requireNonNull(
            value, "value"
        );
        if (value.isBlank()) {
            throw new IllegalArgumentException(
                "InvocationId cannot be blank"
            );
        }
    }

    /**
     * Generate a random invocation ID.
     */
    public static InvocationId generate() {
        return new InvocationId(
            UUID.randomUUID().toString()
        );
    }

    /**
     * Create from a string value.
     */
    public static InvocationId of(
        String value
    ) {
        return new InvocationId(value);
    }

    @Override
    public String toString() {
        return value;
    }
}
