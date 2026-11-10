package io.github.regalpine.agentforge.runtime.signal;

import io.github.regalpine.agentforge.kernel.EntityId;
import io.github.regalpine.agentforge.event.ExecutionId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for G-02: WAIT/HUMAN persistent wait/signal.
 */
class WaitSignalTest {

    private InMemorySignalStore store;
    private WaitSignalManager manager;

    @BeforeEach
    void setUp() {
        store = new InMemorySignalStore();
        manager = new WaitSignalManager(store);
    }

    @Test
    void shouldCreateAndWaitForSignal() {
        ExecutionId execId = new ExecutionId("exec-wait-1");
        EntityId waitNode = new EntityId("wait-node-1");

        WaitSignal signal = manager.createWait(execId, waitNode, SignalType.TIMER, Map.of("timeout", "30s"));
        assertNotNull(signal.signalId());
        assertEquals(SignalStatus.PENDING, signal.status());

        assertTrue(manager.hasPendingSignals(execId));
        assertEquals(1, manager.pendingSignals(execId).size());
    }

    @Test
    void shouldResolvePendingSignal() {
        ExecutionId execId = new ExecutionId("exec-wait-2");
        EntityId humanNode = new EntityId("human-node-1");

        WaitSignal signal = manager.createWait(execId, humanNode, SignalType.HUMAN, Map.of("task", "approve"));
        assertTrue(manager.hasPendingSignals(execId));

        boolean resolved = manager.resolveSignal(signal.signalId(), Map.of("approved", true));
        assertTrue(resolved);
        assertFalse(manager.hasPendingSignals(execId));

        var found = store.find(signal.signalId());
        assertTrue(found.isPresent());
        assertEquals(SignalStatus.RESOLVED, found.get().status());
        assertNotNull(found.get().resolvedAt());
        assertEquals(true, found.get().payload().get("approved"));
    }

    @Test
    void shouldCancelSignal() {
        ExecutionId execId = new ExecutionId("exec-wait-3");
        EntityId waitNode = new EntityId("wait-node-3");

        WaitSignal signal = manager.createWait(execId, waitNode, SignalType.EVENT, Map.of());
        boolean cancelled = manager.cancelSignal(signal.signalId());
        assertTrue(cancelled);
        assertFalse(manager.hasPendingSignals(execId));

        var found = store.find(signal.signalId());
        assertEquals(SignalStatus.CANCELLED, found.get().status());
    }

    @Test
    void shouldNotResolveAlreadyResolvedSignal() {
        ExecutionId execId = new ExecutionId("exec-wait-4");
        EntityId waitNode = new EntityId("wait-node-4");

        WaitSignal signal = manager.createWait(execId, waitNode, SignalType.NAMED, Map.of());
        assertTrue(manager.resolveSignal(signal.signalId(), Map.of()));
        assertFalse(manager.resolveSignal(signal.signalId(), Map.of()));
    }

    @Test
    void shouldNotResolveNonExistentSignal() {
        assertFalse(manager.resolveSignal("non-existent", Map.of()));
    }
}
