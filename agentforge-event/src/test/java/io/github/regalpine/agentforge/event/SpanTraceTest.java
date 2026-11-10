package io.github.regalpine.agentforge.event;


import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Span / SpanTracker / ExecutionTrace 测试。
 */
class SpanTraceTest {

    private SpanTracker tracker;

    @BeforeEach
    void setup() {
        EventEnvelope.resetSequence();
        tracker = new SpanTracker();
    }

    @Test
    void shouldCreateSpanWithBuilder() {
        Span span = Span.builder()
            .kind(SpanKind.AGENT)
            .executionId("exec-1")
            .agentId("agent-1")
            .build();

        assertNotNull(span.spanId());
        assertEquals(SpanKind.AGENT, span.kind());
        assertEquals(SpanStatus.RUNNING, span.status());
        assertNotNull(span.startTime());
    }

    @Test
    void shouldTrackSpanHierarchy() {
        // Agent Span
        Span agentSpan = tracker.startSpan(
            SpanKind.AGENT, "exec-1", "agent-1"
        );
        assertEquals(
            agentSpan.spanId(),
            tracker.currentSpanId()
        );

        // Skill Span (child of Agent)
        Span skillSpan = tracker.startSpan(
            SpanKind.SKILL, "exec-1", "agent-1"
        );
        assertEquals(
            agentSpan.spanId(),
            skillSpan.parentSpanId()
        );

        // Tool Span (child of Skill)
        Span toolSpan = tracker.startSpan(
            SpanKind.TOOL, "exec-1", "agent-1"
        );
        assertEquals(
            skillSpan.spanId(),
            toolSpan.parentSpanId()
        );

        // Complete in reverse order
        tracker.completeSpan(SpanStatus.COMPLETED);
        tracker.completeSpan(SpanStatus.COMPLETED);
        tracker.completeSpan(SpanStatus.COMPLETED);

        assertNull(tracker.currentSpanId());
        assertEquals(3, tracker.allSpans().size());
    }

    @Test
    void shouldFindRootSpans() {
        Span root = tracker.startSpan(
            SpanKind.AGENT, "exec-1", "a1"
        );
        tracker.startSpan(
            SpanKind.SKILL, "exec-1", "a1"
        );
        tracker.completeSpan(SpanStatus.COMPLETED);
        tracker.completeSpan(SpanStatus.COMPLETED);

        List<Span> roots = tracker.rootSpans();
        assertEquals(1, roots.size());
        assertEquals(
            root.spanId(),
            roots.get(0).spanId()
        );
    }

    @Test
    void shouldFindChildren() {
        Span parent = tracker.startSpan(
            SpanKind.AGENT, "exec-1", "a1"
        );
        tracker.startSpan(
            SpanKind.TOOL, "exec-1", "a1"
        );
        tracker.completeSpan(SpanStatus.COMPLETED);
        tracker.completeSpan(SpanStatus.COMPLETED);

        List<Span> children =
            tracker.children(parent.spanId());
        assertEquals(1, children.size());
        assertEquals(
            SpanKind.TOOL,
            children.get(0).kind()
        );
    }

    @Test
    void shouldThrowOnCompleteWithoutSpan() {
        assertThrows(
            IllegalStateException.class,
            () -> tracker.completeSpan(
                SpanStatus.COMPLETED
            )
        );
    }

    @Test
    void shouldBuildExecutionTrace() {
        String execId = "exec-1";
        String traceId = "trace-1";

        List<EventEnvelope> events = List.of(
            EventEnvelope.builder()
                .eventType("execution.started")
                .executionId(execId)
                .traceId(traceId)
                .timestamp(Instant.now())
                .sequence(1)
                .build(),
            EventEnvelope.builder()
                .eventType("agent.started")
                .executionId(execId)
                .traceId(traceId)
                .agentId("a1")
                .timestamp(Instant.now())
                .sequence(2)
                .build(),
            EventEnvelope.builder()
                .eventType("execution.completed")
                .executionId(execId)
                .traceId(traceId)
                .timestamp(Instant.now())
                .sequence(3)
                .build()
        );

        ExecutionTrace trace =
            ExecutionTrace.fromEvents(execId, events);

        assertEquals(execId, trace.executionId());
        assertEquals(traceId, trace.traceId());
        assertEquals("a1", trace.agentId());
        assertEquals(3, trace.events().size());
        assertFalse(trace.hasFailed());
    }

    @Test
    void shouldDetectFailedTrace() {
        List<EventEnvelope> events = List.of(
            EventEnvelope.builder()
                .eventType("execution.started")
                .executionId("e1")
                .timestamp(Instant.now())
                .sequence(1)
                .build(),
            EventEnvelope.builder()
                .eventType("agent.failed")
                .executionId("e1")
                .timestamp(Instant.now())
                .sequence(2)
                .build()
        );

        ExecutionTrace trace =
            ExecutionTrace.fromEvents("e1", events);
        assertTrue(trace.hasFailed());
    }

    @Test
    void shouldFilterEventsByType() {
        List<EventEnvelope> events = List.of(
            EventEnvelope.builder()
                .eventType("tool.started")
                .executionId("e1")
                .timestamp(Instant.now())
                .sequence(1)
                .build(),
            EventEnvelope.builder()
                .eventType("model.delta")
                .executionId("e1")
                .timestamp(Instant.now())
                .sequence(2)
                .build(),
            EventEnvelope.builder()
                .eventType("tool.completed")
                .executionId("e1")
                .timestamp(Instant.now())
                .sequence(3)
                .build()
        );

        ExecutionTrace trace =
            ExecutionTrace.fromEvents("e1", events);

        List<EventEnvelope> toolEvents =
            trace.eventsOfType(
                "tool.started",
                "tool.completed"
            );
        assertEquals(2, toolEvents.size());
    }

    @Test
    void shouldBuildTraceIncrementally() {
        ExecutionTrace trace =
            ExecutionTrace.traceBuilder("exec-1")
                .traceId("trace-1")
                .agentId("a1")
                .parentExecutionId("exec-0")
                .addEvent(EventEnvelope.builder()
                    .eventType("execution.started")
                    .executionId("exec-1")
                    .traceId("trace-1")
                    .timestamp(Instant.now())
                    .sequence(1)
                    .build())
                .addSpan(Span.builder()
                    .kind(SpanKind.AGENT)
                    .executionId("exec-1")
                    .build())
                .output("result", "ok")
                .build();

        assertEquals("exec-1", trace.executionId());
        assertEquals("trace-1", trace.traceId());
        assertEquals("exec-0",
            trace.parentExecutionId());
        assertEquals(1, trace.events().size());
        assertEquals(1, trace.spans().size());
        assertEquals("ok", trace.outputs().get("result"));
    }

    @Test
    void shouldGetSpansForExecution() {
        tracker.startSpan(
            SpanKind.AGENT, "exec-1", "a1"
        );
        tracker.completeSpan(SpanStatus.COMPLETED);
        tracker.startSpan(
            SpanKind.AGENT, "exec-2", "a2"
        );
        tracker.completeSpan(SpanStatus.COMPLETED);

        assertEquals(
            1,
            tracker.spansFor("exec-1").size()
        );
        assertEquals(
            1,
            tracker.spansFor("exec-2").size()
        );
    }
}
