package io.github.regalpine.agentforge.hardening.conformance;

import io.github.regalpine.agentforge.event.ExecutionState;
import io.github.regalpine.agentforge.event.ExecutionStatus;
import io.github.regalpine.agentforge.event.ExecutionId;

import io.github.regalpine.agentforge.hardening.*;
import io.github.regalpine.agentforge.kernel.EntityId;
import io.github.regalpine.agentforge.runtime.*;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * conformance-execution: Lifecycle
 * / State / Delta / Result / Event.
 * AF-RELEASE-02 §4, T-H6.5.
 */
class ExecutionConformanceTest {

    @Test
    void afExec001_stateCreation() {
        var mgr = new InMemoryStateManager();
        var id = new ExecutionId("exec-001");
        var state = new ExecutionState(
            id, ExecutionStatus.READY,
            new EntityId("start"),
            Map.of(), Set.of(), Set.of(), 0
        );
        mgr.create(state);

        var loaded = mgr.get(id);
        assertTrue(loaded.isPresent());
        assertEquals(ExecutionStatus.READY, loaded.get().status());
    }

    @Test
    void afExec002_stateTransition() {
        var mgr = new InMemoryStateManager();
        var id = new ExecutionId("exec-002");
        mgr.create(new ExecutionState(
            id, ExecutionStatus.READY,
            new EntityId("start"),
            Map.of(), Set.of(), Set.of(), 0
        ));

        boolean updated = mgr.compareAndSet(
            id, 0,
            new ExecutionState(
                id, ExecutionStatus.RUNNING,
                new EntityId("node-1"),
                Map.of(), Set.of(), Set.of(), 1
            )
        );
        assertTrue(updated);
        assertEquals(
            ExecutionStatus.RUNNING,
            mgr.get(id).orElseThrow().status()
        );
    }

    @Test
    void afExec003_duplicateStateThrows() {
        var mgr = new InMemoryStateManager();
        var id = new ExecutionId("exec-dup");
        var state = new ExecutionState(
            id, ExecutionStatus.READY,
            new EntityId("start"),
            Map.of(), Set.of(), Set.of(), 0
        );
        mgr.create(state);
        assertThrows(
            IllegalStateException.class,
            () -> mgr.create(state)
        );
    }

    @Test
    void afExec004_missingStateReturnsEmpty() {
        var mgr = new InMemoryStateManager();
        assertTrue(
            mgr.get(new ExecutionId("no-such")).isEmpty()
        );
    }

    @Test
    void afExec005_executionIdGenerate() {
        var id1 = ExecutionId.generate();
        var id2 = ExecutionId.generate();
        assertNotNull(id1);
        assertNotNull(id2);
        assertNotEquals(id1, id2);
    }
}
