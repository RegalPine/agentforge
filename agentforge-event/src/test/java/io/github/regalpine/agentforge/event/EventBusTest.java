package io.github.regalpine.agentforge.event;


import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * EventBus / EventFilter / EventSubscriber 测试。
 */
class EventBusTest {

    @Test
    void shouldPublishAndSubscribe() {
        InMemoryEventBus bus = new InMemoryEventBus();
        var received =
            new java.util.ArrayList<EventEnvelope>();

        bus.subscribe(received::add);

        EventEnvelope event = EventEnvelope.builder()
            .eventType("agent.started")
            .executionId("exec-1")
            .timestamp(Instant.now())
            .sequence(1)
            .build();

        bus.publish(event);

        assertEquals(1, received.size());
        assertEquals("agent.started",
            received.get(0).eventType());
    }

    @Test
    void shouldFilterByExecutionId() {
        InMemoryEventBus bus = new InMemoryEventBus();
        var received =
            new java.util.ArrayList<EventEnvelope>();

        bus.subscribe(
            EventFilter.forExecution("exec-1"),
            received::add
        );

        bus.publish(EventEnvelope.builder()
            .eventType("e1")
            .executionId("exec-1")
            .timestamp(Instant.now())
            .sequence(1)
            .build());
        bus.publish(EventEnvelope.builder()
            .eventType("e2")
            .executionId("exec-2")
            .timestamp(Instant.now())
            .sequence(2)
            .build());

        assertEquals(1, received.size());
        assertEquals("exec-1",
            received.get(0).executionId());
    }

    @Test
    void shouldFilterByEventTypes() {
        InMemoryEventBus bus = new InMemoryEventBus();
        var received =
            new java.util.ArrayList<EventEnvelope>();

        bus.subscribe(
            EventFilter.forTypes(
                "tool.started",
                "tool.completed"
            ),
            received::add
        );

        bus.publish(EventEnvelope.builder()
            .eventType("tool.started")
            .timestamp(Instant.now())
            .sequence(1)
            .build());
        bus.publish(EventEnvelope.builder()
            .eventType("model.delta")
            .timestamp(Instant.now())
            .sequence(2)
            .build());
        bus.publish(EventEnvelope.builder()
            .eventType("tool.completed")
            .timestamp(Instant.now())
            .sequence(3)
            .build());

        assertEquals(2, received.size());
    }

    @Test
    void shouldCancelSubscription() {
        InMemoryEventBus bus = new InMemoryEventBus();
        var received =
            new java.util.ArrayList<EventEnvelope>();

        var sub = bus.subscribe(received::add);

        bus.publish(EventEnvelope.builder()
            .eventType("e1")
            .timestamp(Instant.now())
            .sequence(1)
            .build());
        assertEquals(1, received.size());

        sub.cancel();

        bus.publish(EventEnvelope.builder()
            .eventType("e2")
            .timestamp(Instant.now())
            .sequence(2)
            .build());
        assertEquals(1, received.size());
    }

    @Test
    void shouldTrackHistory() {
        InMemoryEventBus bus = new InMemoryEventBus();

        bus.publish(EventEnvelope.builder()
            .eventType("e1")
            .timestamp(Instant.now())
            .sequence(1)
            .build());
        bus.publish(EventEnvelope.builder()
            .eventType("e2")
            .timestamp(Instant.now())
            .sequence(2)
            .build());

        assertEquals(2, bus.history().size());
    }

    @Test
    void shouldFilterByAgentId() {
        EventFilter filter =
            EventFilter.forAgent("agent-1");

        EventEnvelope match = EventEnvelope.builder()
            .eventType("test")
            .agentId("agent-1")
            .timestamp(Instant.now())
            .sequence(1)
            .build();
        EventEnvelope noMatch = EventEnvelope.builder()
            .eventType("test")
            .agentId("agent-2")
            .timestamp(Instant.now())
            .sequence(2)
            .build();

        assertTrue(filter.matches(match));
        assertFalse(filter.matches(noMatch));
    }

    @Test
    void shouldFilterByToolId() {
        EventFilter filter =
            EventFilter.builder()
                .toolId("web-search")
                .build();

        EventEnvelope match = EventEnvelope.builder()
            .eventType("tool.started")
            .payload(Map.of("toolId", "web-search"))
            .timestamp(Instant.now())
            .sequence(1)
            .build();
        EventEnvelope noMatch = EventEnvelope.builder()
            .eventType("tool.started")
            .payload(Map.of("toolId", "calc"))
            .timestamp(Instant.now())
            .sequence(2)
            .build();

        assertTrue(filter.matches(match));
        assertFalse(filter.matches(noMatch));
    }

    @Test
    void allFilterShouldMatchEverything() {
        EventFilter filter = EventFilter.all();
        EventEnvelope event = EventEnvelope.builder()
            .eventType("test")
            .executionId("e1")
            .agentId("a1")
            .timestamp(Instant.now())
            .sequence(1)
            .build();

        assertTrue(filter.matches(event));
    }
}
