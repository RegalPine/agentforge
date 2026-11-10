package io.github.regalpine.agentforge.event;


import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * EventEnvelope 核心测试。
 */
class EventEnvelopeTest {

    @BeforeEach
    void reset() {
        EventEnvelope.resetSequence();
    }

    @Test
    void shouldCreateWithAllFields() {
        Instant now = Instant.now();
        EventEnvelope env = EventEnvelope.builder()
            .eventId("evt-1")
            .eventType("agent.started")
            .eventVersion(1)
            .timestamp(now)
            .executionId("exec-1")
            .parentExecutionId("exec-0")
            .traceId("trace-1")
            .spanId("span-1")
            .parentSpanId("span-0")
            .agentId("agent-1")
            .tenantId("tenant-1")
            .sequence(1)
            .payload(Map.of("key", "value"))
            .metadata(Map.of("env", "test"))
            .build();

        assertEquals("evt-1", env.eventId());
        assertEquals("agent.started", env.eventType());
        assertEquals(1, env.eventVersion());
        assertEquals(now, env.timestamp());
        assertEquals("exec-1", env.executionId());
        assertEquals("exec-0", env.parentExecutionId());
        assertEquals("trace-1", env.traceId());
        assertEquals("span-1", env.spanId());
        assertEquals("span-0", env.parentSpanId());
        assertEquals("agent-1", env.agentId());
        assertEquals("tenant-1", env.tenantId());
        assertEquals(1, env.sequence());
        assertEquals("value", env.payload().get("key"));
        assertEquals("test", env.metadata().get("env"));
    }

    @Test
    void shouldAutoGenerateEventId() {
        EventEnvelope env = EventEnvelope.builder()
            .eventType("test.event")
            .timestamp(Instant.now())
            .sequence(1)
            .build();

        assertNotNull(env.eventId());
        assertTrue(env.eventId().startsWith("evt-"));
    }

    @Test
    void shouldAutoIncrementSequence() {
        EventEnvelope.resetSequence();
        EventEnvelope e1 = EventEnvelope.builder()
            .eventType("t1").timestamp(Instant.now())
            .build();
        EventEnvelope e2 = EventEnvelope.builder()
            .eventType("t2").timestamp(Instant.now())
            .build();

        assertEquals(1, e1.sequence());
        assertEquals(2, e2.sequence());
    }

    @Test
    void shouldRejectJavaClassName() {
        assertThrows(
            IllegalArgumentException.class,
            () -> EventEnvelope.builder()
                .eventType(
                    "com.example.MyEvent"
                )
                .timestamp(Instant.now())
                .build()
        );
    }

    @Test
    void shouldDeriveCategory() {
        EventEnvelope env = EventEnvelope.builder()
            .eventType("tool.started")
            .timestamp(Instant.now())
            .sequence(1)
            .build();
        assertEquals(
            EventCategory.TOOL,
            env.category()
        );
    }

    @Test
    void shouldIdentifyCriticalEvents() {
        EventEnvelope critical =
            EventEnvelope.builder()
                .eventType("execution.failed")
                .timestamp(Instant.now())
                .sequence(1)
                .build();
        assertTrue(critical.isCritical());

        EventEnvelope nonCritical =
            EventEnvelope.builder()
                .eventType("model.delta")
                .timestamp(Instant.now())
                .sequence(2)
                .build();
        assertFalse(nonCritical.isCritical());
    }

    @Test
    void shouldDefensiveCopyPayload() {
        EventEnvelope env = EventEnvelope.builder()
            .eventType("test")
            .timestamp(Instant.now())
            .sequence(1)
            .payload(Map.of("k", "v"))
            .build();

        assertThrows(
            UnsupportedOperationException.class,
            () -> env.payload().put("x", "y")
        );
    }

    @Test
    void shouldHandleNullPayloadAsEmpty() {
        EventEnvelope env = EventEnvelope.builder()
            .eventType("test")
            .timestamp(Instant.now())
            .sequence(1)
            .build();

        assertNotNull(env.payload());
        assertTrue(env.payload().isEmpty());
    }

    @Test
    void shouldSupportToBuilder() {
        EventEnvelope original =
            EventEnvelope.builder()
                .eventId("evt-1")
                .eventType("agent.started")
                .timestamp(Instant.now())
                .sequence(1)
                .agentId("a1")
                .build();

        EventEnvelope modified =
            original.toBuilder()
                .eventType("agent.completed")
                .build();

        assertEquals("evt-1", modified.eventId());
        assertEquals(
            "agent.completed",
            modified.eventType()
        );
        assertEquals("a1", modified.agentId());
    }

    @Test
    void shouldRequireEventType() {
        assertThrows(
            NullPointerException.class,
            () -> EventEnvelope.builder()
                .timestamp(Instant.now())
                .build()
        );
    }
}
