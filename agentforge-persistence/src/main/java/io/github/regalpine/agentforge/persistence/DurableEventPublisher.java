package io.github.regalpine.agentforge.persistence;

import io.github.regalpine.agentforge.event.EventPublisher;
import io.github.regalpine.agentforge.event.ExecutionEvent;

public final class DurableEventPublisher
    implements EventPublisher {

    private final EventStore eventStore;
    private final OutboxStore outboxStore;

    public DurableEventPublisher(
        EventStore eventStore,
        OutboxStore outboxStore
    ) {
        this.eventStore = eventStore;
        this.outboxStore = outboxStore;
    }

    @Override
    public void publish(
        ExecutionEvent event
    ) {
        eventStore.append(event);

        outboxStore.append(
            new OutboxMessage(
                event.executionId().value()
                    + "-"
                    + event.sequence(),
                event.executionId().value(),
                event.type(),
                event.payload().toString(),
                event.timestamp(),
                false
            )
        );
    }
}
