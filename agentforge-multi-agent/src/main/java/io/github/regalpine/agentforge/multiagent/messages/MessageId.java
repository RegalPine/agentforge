package io.github.regalpine.agentforge.multiagent.messages;

import java.util.Objects;
import java.util.UUID;

/**
 * MessageId — AF-ENHANCE-08 §32.
 *
 * Unique identity for an agent
 * message.
 */
public record MessageId(
    String value
) {
    public MessageId {
        Objects.requireNonNull(
            value, "value"
        );
        if (value.isBlank()) {
            throw new IllegalArgumentException(
                "MessageId cannot be blank"
            );
        }
    }

    public static MessageId generate() {
        return new MessageId(
            UUID.randomUUID().toString()
        );
    }

    public static MessageId of(String v) {
        return new MessageId(v);
    }

    @Override
    public String toString() {
        return value;
    }
}
