package io.github.regalpine.agentforge.persistence.postgresql;

import io.github.regalpine.agentforge.event.Execution;
import io.github.regalpine.agentforge.event.ExecutionState;
import io.github.regalpine.agentforge.event.ExecutionStatus;
import io.github.regalpine.agentforge.event.ExecutionId;
import io.github.regalpine.agentforge.event.ExecutionEvent;

import io.github.regalpine.agentforge.kernel.EntityId;
import io.github.regalpine.agentforge.kernel.Version;
import io.github.regalpine.agentforge.persistence.*;
import io.github.regalpine.agentforge.runtime.*;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.*;

import java.time.Instant;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for all JDBC persistence stores using H2 in PostgreSQL compatibility mode.
 * Validates G-04 (PostgreSQL adapter) and G-05 (CommitPort transaction protocol).
 */
class JdbcPersistenceIntegrationTest {

    private static PostgresPersistenceModule module;

    @BeforeAll
    static void setUpModule() {
        JdbcDataSource ds = new JdbcDataSource();
        ds.setURL("jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1;MODE=PostgreSQL");
        ds.setUser("sa");
        ds.setPassword("");
        module = PostgresPersistenceModule.create(ds);
    }

    // ── ExecutionStateStore ──

    @Test
    void shouldCreateAndFindExecutionState() {
        ExecutionId id = new ExecutionId("state-1");
        ExecutionState state = new ExecutionState(
                id, ExecutionStatus.CREATED, new EntityId("start"),
                Map.of("key", "value"), Set.of(new EntityId("start")), Set.of(), 0
        );
        module.executionStateStore().create(state);

        var found = module.executionStateStore().find(id);
        assertTrue(found.isPresent());
        assertEquals(ExecutionStatus.CREATED, found.get().status());
        assertEquals("start", found.get().currentNode().value());
        assertEquals(0L, found.get().version());
    }

    @Test
    void shouldCASUpdateExecutionState() {
        ExecutionId id = new ExecutionId("state-cas");
        ExecutionState initial = new ExecutionState(
                id, ExecutionStatus.CREATED, null, Map.of(), Set.of(), Set.of(), 0
        );
        module.executionStateStore().create(initial);

        ExecutionState running = new ExecutionState(
                id, ExecutionStatus.RUNNING, new EntityId("node-1"),
                Map.of("step", "1"), Set.of(new EntityId("start")), Set.of(), 1
        );
        boolean ok = module.executionStateStore().compareAndSet(id, 0, running);
        assertTrue(ok);

        var found = module.executionStateStore().find(id);
        assertTrue(found.isPresent());
        assertEquals(ExecutionStatus.RUNNING, found.get().status());
        assertEquals(1L, found.get().version());
    }

    @Test
    void shouldRejectCASWithWrongVersion() {
        ExecutionId id = new ExecutionId("state-cas-fail");
        ExecutionState initial = new ExecutionState(
                id, ExecutionStatus.CREATED, null, Map.of(), Set.of(), Set.of(), 0
        );
        module.executionStateStore().create(initial);

        ExecutionState newState = new ExecutionState(
                id, ExecutionStatus.RUNNING, null, Map.of(), Set.of(), Set.of(), 1
        );
        // Wrong expected version
        boolean ok = module.executionStateStore().compareAndSet(id, 99, newState);
        assertFalse(ok);
    }

    // ── EventStore ──

    @Test
    void shouldAppendAndListEvents() {
        ExecutionId id = new ExecutionId("events-1");
        module.eventStore().append(new ExecutionEvent(id, 1, Instant.now(), "NODE_STARTED", Map.of("node", "n1")));
        module.eventStore().append(new ExecutionEvent(id, 2, Instant.now(), "NODE_COMPLETED", Map.of("node", "n1")));

        var events = module.eventStore().list(id);
        assertEquals(2, events.size());
        assertEquals("NODE_STARTED", events.get(0).type());
        assertEquals("NODE_COMPLETED", events.get(1).type());
    }

    @Test
    void shouldListEventsAfterSequence() {
        ExecutionId id = new ExecutionId("events-after");
        module.eventStore().append(new ExecutionEvent(id, 1, Instant.now(), "E1", Map.of()));
        module.eventStore().append(new ExecutionEvent(id, 2, Instant.now(), "E2", Map.of()));
        module.eventStore().append(new ExecutionEvent(id, 3, Instant.now(), "E3", Map.of()));

        var events = module.eventStore().listAfter(id, 1);
        assertEquals(2, events.size());
        assertEquals("E2", events.get(0).type());
    }

    // ── CheckpointStore ──

    @Test
    void shouldSaveAndFindLatestCheckpoint() {
        ExecutionId id = new ExecutionId("ckpt-1");
        module.checkpointStore().save(new Checkpoint("ckpt-1-a", id, 1, 1, new EntityId("n1"), ExecutionStatus.RUNNING, Map.of("data", "v1"), null, Instant.now()));
        module.checkpointStore().save(new Checkpoint("ckpt-1-b", id, 2, 2, new EntityId("n2"), ExecutionStatus.RUNNING, Map.of("data", "v2"), null, Instant.now()));

        var latest = module.checkpointStore().latest(id);
        assertTrue(latest.isPresent());
        assertEquals(2L, latest.get().stateVersion());
        assertEquals("n2", latest.get().currentNode().value());

        var all = module.checkpointStore().list(id);
        assertEquals(2, all.size());
    }

    // ── SnapshotStore ──

    @Test
    void shouldSaveAndFindLatestSnapshot() {
        ExecutionId id = new ExecutionId("snap-1");
        module.snapshotStore().save(new Snapshot("snap-1-a", id, 5, 3, new EntityId("n1"), ExecutionStatus.RUNNING, Map.of("state", "old"), null, Instant.now()));
        module.snapshotStore().save(new Snapshot("snap-1-b", id, 10, 6, new EntityId("n2"), ExecutionStatus.RUNNING, Map.of("state", "new"), null, Instant.now()));

        var latest = module.snapshotStore().latest(id);
        assertTrue(latest.isPresent());
        assertEquals(10L, latest.get().eventSequence());
        assertEquals("new", latest.get().state().get("state"));
    }

    // ── OutboxStore ──

    @Test
    void shouldAppendListAndMarkPublished() {
        module.outboxStore().append(new OutboxMessage("msg-1", "agg-1", "STATE_CHANGED", "{}", Instant.now(), false));
        module.outboxStore().append(new OutboxMessage("msg-2", "agg-2", "STATE_CHANGED", "{}", Instant.now(), false));

        var unpublished = module.outboxStore().unpublished(10);
        assertEquals(2, unpublished.size());

        module.outboxStore().markPublished("msg-1");

        var remaining = module.outboxStore().unpublished(10);
        assertEquals(1, remaining.size());
        assertEquals("msg-2", remaining.get(0).messageId());
    }

    // ── InvocationStore ──

    @Test
    void shouldCreateFindAndUpdateInvocation() {
        StoredInvocation inv = new StoredInvocation(
                "inv-1", new ExecutionId("exec-1"), new EntityId("cap-1"),
                new Version("1.0.0"), "idem-key-1", "STARTED",
                Instant.now(), null, null
        );
        module.invocationStore().create(inv);

        var found = module.invocationStore().find("inv-1");
        assertTrue(found.isPresent());
        assertEquals("STARTED", found.get().status());

        var byKey = module.invocationStore().findByIdempotencyKey("idem-key-1");
        assertTrue(byKey.isPresent());
        assertEquals("inv-1", byKey.get().invocationId());

        StoredInvocation completed = new StoredInvocation(
                "inv-1", new ExecutionId("exec-1"), new EntityId("cap-1"),
                new Version("1.0.0"), "idem-key-1", "COMPLETED",
                inv.startedAt(), Instant.now(), "result-ref-1"
        );
        module.invocationStore().update(completed);

        var updated = module.invocationStore().find("inv-1");
        assertTrue(updated.isPresent());
        assertEquals("COMPLETED", updated.get().status());
        assertEquals("result-ref-1", updated.get().resultReference());
    }

    // ── ExecutionStore ──

    @Test
    void shouldCreateFindAndUpdateExecution() {
        Execution exec = new Execution(
                new ExecutionId("exec-pg-1"), new EntityId("graph-1"),
                new Version("1.0.0"), "fp-123", ExecutionStatus.CREATED,
                Instant.now(), null, null
        );
        module.executionStore().create(exec);

        var found = module.executionStore().find(new ExecutionId("exec-pg-1"));
        assertTrue(found.isPresent());
        assertEquals(ExecutionStatus.CREATED, found.get().status());

        Execution running = new Execution(
                new ExecutionId("exec-pg-1"), new EntityId("graph-1"),
                new Version("1.0.0"), "fp-123", ExecutionStatus.RUNNING,
                exec.createdAt(), Instant.now(), null
        );
        module.executionStore().update(running);

        var updated = module.executionStore().find(new ExecutionId("exec-pg-1"));
        assertTrue(updated.isPresent());
        assertEquals(ExecutionStatus.RUNNING, updated.get().status());
        assertNotNull(updated.get().startedAt());
    }

    // ── CommitPort (G-05) ──

    @Test
    void shouldAtomicCommitStateAndEvent() {
        ExecutionId id = new ExecutionId("commit-1");
        ExecutionState initial = new ExecutionState(
                id, ExecutionStatus.CREATED, null, Map.of(), Set.of(), Set.of(), 0
        );
        module.executionStateStore().create(initial);

        ExecutionState newState = new ExecutionState(
                id, ExecutionStatus.RUNNING, new EntityId("n1"),
                Map.of(), Set.of(new EntityId("start")), Set.of(), 1
        );
        ExecutionEvent event = new ExecutionEvent(id, 1, Instant.now(), "NODE_STARTED", Map.of("node", "n1"));

        boolean ok = module.commitPort().commit(new CommitPort.CommitCommand(newState, event, 0));
        assertTrue(ok);

        // State should be updated
        var state = module.executionStateStore().find(id);
        assertTrue(state.isPresent());
        assertEquals(ExecutionStatus.RUNNING, state.get().status());
        assertEquals(1L, state.get().version());

        // Event should be appended
        var events = module.eventStore().list(id);
        assertEquals(1, events.size());
        assertEquals("NODE_STARTED", events.get(0).type());
    }

    @Test
    void shouldRejectCommitOnVersionConflict() {
        ExecutionId id = new ExecutionId("commit-conflict");
        ExecutionState initial = new ExecutionState(
                id, ExecutionStatus.CREATED, null, Map.of(), Set.of(), Set.of(), 0
        );
        module.executionStateStore().create(initial);

        // First commit succeeds
        ExecutionState s1 = new ExecutionState(
                id, ExecutionStatus.RUNNING, new EntityId("n1"),
                Map.of(), Set.of(), Set.of(), 1
        );
        ExecutionEvent e1 = new ExecutionEvent(id, 1, Instant.now(), "E1", Map.of());
        assertTrue(module.commitPort().commit(new CommitPort.CommitCommand(s1, e1, 0)));

        // Second commit with stale version should fail
        ExecutionState s2 = new ExecutionState(
                id, ExecutionStatus.COMPLETED, null,
                Map.of(), Set.of(), Set.of(), 2
        );
        ExecutionEvent e2 = new ExecutionEvent(id, 2, Instant.now(), "E2", Map.of());
        assertFalse(module.commitPort().commit(new CommitPort.CommitCommand(s2, e2, 0)));

        // State should still be at version 1
        var state = module.executionStateStore().find(id);
        assertEquals(1L, state.get().version());
        assertEquals(ExecutionStatus.RUNNING, state.get().status());
    }
}
