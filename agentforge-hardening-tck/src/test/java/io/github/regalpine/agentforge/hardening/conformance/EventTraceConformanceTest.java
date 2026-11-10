package io.github.regalpine.agentforge.hardening.conformance;

import io.github.regalpine.agentforge.event.*;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * conformance-event-trace:
 * EventEnvelope / EventCategory /
 * Span / Trace.
 * AF-AD-16 §6.2, T-X12.2.
 */
class EventTraceConformanceTest {

    // ── EventEnvelope ──

    @Test
    void afEvt001_envelopeCreation() {
        var env = EventEnvelope.builder()
            .eventType("execution.started")
            .executionId("exec-1")
            .traceId("trace-1")
            .spanId("span-1")
            .build();
        assertNotNull(env.eventId());
        assertEquals(
            "execution.started",
            env.eventType()
        );
        assertEquals("exec-1",
            env.executionId());
        assertTrue(env.sequence() > 0);
    }

    @Test
    void afEvt002_envelopeRejectsJavaClass() {
        assertThrows(
            IllegalArgumentException.class,
            () -> EventEnvelope.builder()
                .eventType(
                    "com.example.MyEvent")
                .build()
        );
    }

    @Test
    void afEvt003_envelopeDotNotation() {
        assertThrows(
            IllegalArgumentException.class,
            () -> EventEnvelope.builder()
                .eventType("my$Event")
                .build()
        );
    }

    // ── EventCategory ──

    @Test
    void afEvt004_elevenCategories() {
        assertEquals(
            11, EventCategory.values().length
        );
        assertNotNull(
            EventCategory.LIFECYCLE
        );
        assertNotNull(
            EventCategory.EXECUTION
        );
        assertNotNull(
            EventCategory.SKILL
        );
        assertNotNull(
            EventCategory.POLICY
        );
        assertNotNull(
            EventCategory.HUMAN
        );
        assertNotNull(
            EventCategory.ERROR
        );
    }

    @Test
    void afEvt005_categoryFromEventType() {
        assertEquals(
            EventCategory.EXECUTION,
            EventCategory.fromEventType(
                "execution.started")
        );
        assertEquals(
            EventCategory.SKILL,
            EventCategory.fromEventType(
                "skill.invoked")
        );
        assertEquals(
            EventCategory.POLICY,
            EventCategory.fromEventType(
                "policy.denied")
        );
        assertEquals(
            EventCategory.HUMAN,
            EventCategory.fromEventType(
                "human.approval.received")
        );
    }

    // ── Span ──

    @Test
    void afEvt006_spanCreation() {
        var span = Span.builder()
            .kind(SpanKind.AGENT)
            .executionId("exec-1")
            .agentId("agent-1")
            .build();
        assertNotNull(span.spanId());
        assertEquals(
            SpanKind.AGENT, span.kind()
        );
        assertEquals(
            SpanStatus.RUNNING,
            span.status()
        );
    }

    @Test
    void afEvt007_spanKinds() {
        assertEquals(
            7, SpanKind.values().length
        );
        assertNotNull(SpanKind.AGENT);
        assertNotNull(SpanKind.SKILL);
        assertNotNull(SpanKind.TOOL);
        assertNotNull(SpanKind.MODEL);
    }

    // ── ExecutionTrace ──

    @Test
    void afEvt008_traceFromEvents() {
        var e1 = EventEnvelope.builder()
            .eventType("execution.started")
            .executionId("exec-1")
            .traceId("trace-1")
            .agentId("agent-1")
            .build();
        var e2 = EventEnvelope.builder()
            .eventType("execution.completed")
            .executionId("exec-1")
            .traceId("trace-1")
            .build();

        var trace = ExecutionTrace.fromEvents(
            "exec-1", List.of(e1, e2)
        );
        assertEquals("exec-1",
            trace.executionId());
        assertEquals(2,
            trace.events().size());
    }

    @Test
    void afEvt009_traceBuilder() {
        var trace = ExecutionTrace
            .traceBuilder("exec-2")
            .traceId("trace-2")
            .agentId("agent-2")
            .build();
        assertEquals("exec-2",
            trace.executionId());
        assertEquals("trace-2",
            trace.traceId());
    }

    @Test
    void afEvt010_criticalEvents() {
        var env = EventEnvelope.builder()
            .eventType("execution.started")
            .build();
        assertTrue(env.isCritical());

        var nonCrit = EventEnvelope.builder()
            .eventType("model.response")
            .build();
        assertFalse(
            nonCrit.isCritical()
        );
    }
}
