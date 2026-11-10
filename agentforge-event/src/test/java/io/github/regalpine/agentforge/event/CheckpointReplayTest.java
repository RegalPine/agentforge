package io.github.regalpine.agentforge.event;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Checkpoint / ReplayEngine / EventStore /
 * AuditLogger 测试。
 */
class CheckpointReplayTest {

    private InMemoryEnvelopeEventStore store;
    private CheckpointEngine checkpointEngine;
    private ReplayEngine replayEngine;

    @BeforeEach
    void setup() {
        EventEnvelope.resetSequence();
        store = new InMemoryEnvelopeEventStore();
        checkpointEngine = new CheckpointEngine();
        replayEngine = new ReplayEngine(
            store, checkpointEngine
        );
    }

    @Test
    void shouldSaveAndRetrieveCheckpoint() {
        Checkpoint cp = Checkpoint.of(
            "exec-1", 5,
            Map.of("state", "running")
        );
        checkpointEngine.save(cp);

        Checkpoint latest =
            checkpointEngine.latest("exec-1");
        assertNotNull(latest);
        assertEquals("exec-1", latest.executionId());
        assertEquals(5, latest.sequence());
        assertEquals(
            "running",
            latest.state().get("state")
        );
    }

    @Test
    void shouldReturnLatestCheckpoint() {
        checkpointEngine.save(
            Checkpoint.of("exec-1", 3, Map.of())
        );
        checkpointEngine.save(
            Checkpoint.of("exec-1", 7, Map.of())
        );
        checkpointEngine.save(
            Checkpoint.of("exec-1", 5, Map.of())
        );

        Checkpoint latest =
            checkpointEngine.latest("exec-1");
        assertEquals(7, latest.sequence());
    }

    @Test
    void shouldReturnNullForUnknownExecution() {
        assertNull(
            checkpointEngine.latest("unknown")
        );
    }

    @Test
    void shouldRestoreState() {
        checkpointEngine.save(Checkpoint.of(
            "exec-1", 5,
            Map.of("node", "n1", "status", "ok")
        ));

        Map<String, Object> state =
            checkpointEngine.restore("exec-1");
        assertEquals("n1", state.get("node"));
        assertEquals("ok", state.get("status"));
    }

    @Test
    void shouldStateReplayFromCheckpoint() {
        // Store some events
        store.append(EventEnvelope.builder()
            .eventType("execution.started")
            .executionId("exec-1")
            .timestamp(Instant.now())
            .sequence(1)
            .build());
        store.append(EventEnvelope.builder()
            .eventType("agent.started")
            .executionId("exec-1")
            .timestamp(Instant.now())
            .sequence(2)
            .build());
        store.append(EventEnvelope.builder()
            .eventType("tool.started")
            .executionId("exec-1")
            .timestamp(Instant.now())
            .sequence(3)
            .build());

        // Save checkpoint at seq 2
        checkpointEngine.save(Checkpoint.of(
            "exec-1", 2, Map.of()
        ));

        // Replay
        ReplayEngine.ReplayResult result =
            replayEngine.stateReplay("exec-1");

        assertTrue(result.hasCheckpoint());
        assertEquals(1, result.eventCount());
        assertEquals(
            "tool.started",
            result.events().get(0).eventType()
        );
    }

    @Test
    void shouldStateReplayFromStart() {
        store.append(EventEnvelope.builder()
            .eventType("execution.started")
            .executionId("exec-1")
            .timestamp(Instant.now())
            .sequence(1)
            .build());

        ReplayEngine.ReplayResult result =
            replayEngine.stateReplay("exec-1");

        assertFalse(result.hasCheckpoint());
        assertEquals(1, result.eventCount());
    }

    @Test
    void shouldStoreAndReadEvents() {
        store.append(EventEnvelope.builder()
            .eventType("e1")
            .executionId("exec-1")
            .traceId("trace-1")
            .timestamp(Instant.now())
            .sequence(1)
            .build());
        store.append(EventEnvelope.builder()
            .eventType("e2")
            .executionId("exec-1")
            .traceId("trace-1")
            .timestamp(Instant.now())
            .sequence(2)
            .build());
        store.append(EventEnvelope.builder()
            .eventType("e3")
            .executionId("exec-2")
            .traceId("trace-2")
            .timestamp(Instant.now())
            .sequence(3)
            .build());

        assertEquals(
            2, store.read("exec-1").size()
        );
        assertEquals(
            1, store.read("exec-2").size()
        );
        assertEquals(
            1, store.readAfter("exec-1", 1).size()
        );
        assertEquals(
            2, store.readByTrace("trace-1").size()
        );
        assertEquals(
            2, store.count("exec-1")
        );
    }

    @Test
    void shouldAuditLogCaptureAuditEvents() {
        AuditLogger audit = new AuditLogger();

        audit.onEvent(EventEnvelope.builder()
            .eventType("policy.denied")
            .executionId("exec-1")
            .timestamp(Instant.now())
            .sequence(1)
            .build());
        audit.onEvent(EventEnvelope.builder()
            .eventType("model.delta")
            .executionId("exec-1")
            .timestamp(Instant.now())
            .sequence(2)
            .build());
        audit.onEvent(EventEnvelope.builder()
            .eventType("human.approval.required")
            .executionId("exec-1")
            .timestamp(Instant.now())
            .sequence(3)
            .build());

        assertEquals(2, audit.entries().size());
        assertEquals(
            2,
            audit.entriesFor("exec-1").size()
        );
    }

    @Test
    void shouldBuildTraceFromStore() {
        store.append(EventEnvelope.builder()
            .eventType("execution.started")
            .executionId("exec-1")
            .traceId("trace-1")
            .timestamp(Instant.now())
            .sequence(1)
            .build());
        store.append(EventEnvelope.builder()
            .eventType("agent.started")
            .executionId("exec-1")
            .traceId("trace-1")
            .agentId("a1")
            .timestamp(Instant.now())
            .sequence(2)
            .build());

        ExecutionTrace trace =
            replayEngine.trace("exec-1");

        assertEquals("exec-1", trace.executionId());
        assertEquals("trace-1", trace.traceId());
        assertEquals("a1", trace.agentId());
        assertEquals(2, trace.events().size());
    }

    @Test
    void shouldClearCheckpoint() {
        checkpointEngine.save(
            Checkpoint.of("exec-1", 1, Map.of())
        );
        assertEquals(1, checkpointEngine.size());

        checkpointEngine.clear("exec-1");
        assertEquals(0, checkpointEngine.size());
        assertNull(
            checkpointEngine.latest("exec-1")
        );
    }

    @Test
    void shouldClearStore() {
        store.append(EventEnvelope.builder()
            .eventType("e1")
            .timestamp(Instant.now())
            .sequence(1)
            .build());
        assertEquals(1, store.all().size());

        store.clear();
        assertEquals(0, store.all().size());
    }
}
