package io.github.regalpine.agentforge.persistence.memory;

import io.github.regalpine.agentforge.persistence.OutboxMessage;
import io.github.regalpine.agentforge.persistence.OutboxStore;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class InMemoryOutboxStore
    implements OutboxStore {

    private final Map<
        String,
        OutboxMessage
    > messages = new ConcurrentHashMap<>();

    @Override
    public void append(
        OutboxMessage message
    ) {
        OutboxMessage previous =
            messages.putIfAbsent(
                message.messageId(),
                message
            );

        if (previous != null) {
            throw new IllegalStateException(
                "Outbox message already exists: "
                    + message.messageId()
            );
        }
    }

    @Override
    public List<OutboxMessage> unpublished(
        int limit
    ) {
        List<OutboxMessage> result =
            new ArrayList<>();

        for (OutboxMessage msg : messages.values()) {
            if (!msg.published()) {
                result.add(msg);
            }

            if (result.size() >= limit) {
                break;
            }
        }

        return List.copyOf(result);
    }

    @Override
    public void markPublished(
        String messageId
    ) {
        OutboxMessage msg =
            messages.get(messageId);

        if (msg != null) {
            messages.put(
                messageId,
                new OutboxMessage(
                    msg.messageId(),
                    msg.aggregateId(),
                    msg.eventType(),
                    msg.payload(),
                    msg.createdAt(),
                    true
                )
            );
        }
    }
}
