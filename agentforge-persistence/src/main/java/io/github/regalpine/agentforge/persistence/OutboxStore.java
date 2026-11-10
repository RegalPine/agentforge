package io.github.regalpine.agentforge.persistence;

import java.util.List;

public interface OutboxStore {

    void append(
        OutboxMessage message
    );

    List<OutboxMessage> unpublished(
        int limit
    );

    void markPublished(
        String messageId
    );
}
