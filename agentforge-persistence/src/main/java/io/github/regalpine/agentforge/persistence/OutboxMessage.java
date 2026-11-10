package io.github.regalpine.agentforge.persistence;

import java.time.Instant;

public record OutboxMessage(
    String messageId,
    String aggregateId,
    String eventType,
    String payload,
    Instant createdAt,
    boolean published
) {
}
