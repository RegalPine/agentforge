package io.github.regalpine.agentforge.runtime;

import io.github.regalpine.agentforge.event.ExecutionState;
import io.github.regalpine.agentforge.event.ExecutionStatus;
import io.github.regalpine.agentforge.event.ExecutionId;
import io.github.regalpine.agentforge.event.ExecutionEvent;
import io.github.regalpine.agentforge.event.EventPublisher;

import io.github.regalpine.agentforge.kernel.EntityId;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/** 批次 3 原子提交协议测试：验证扩展 CommitCommand 的原子工作单元。 */
class AtomicCommitProtocolTest {

    private record EventLog(java.util.List<ExecutionEvent> events) {
        EventLog() { this(new java.util.ArrayList<>()); }
        synchronized void add(ExecutionEvent e) { events.add(e); }
        synchronized int size() { return events.size(); }
    }

    private static final class CollectingPublisher implements EventPublisher {
        private final EventLog log = new EventLog();
        @Override public synchronized void publish(ExecutionEvent event) { log.add(event); }
        synchronized List<ExecutionEvent> events() { return List.copyOf(log.events()); }
        synchronized int count() { return log.size(); }
    }

    @Test
    void extendedCommitStoresActivationsAndDispatchesAtomically() {
        var publisher = new CollectingPublisher();
        var port = new InMemoryCommitPort(publisher);
        var id = new ExecutionId("atomic-1");
        port.create(new ExecutionState(id, ExecutionStatus.CREATED, null, Map.of(), Set.of(), Set.of(), 0));

        var newState = new ExecutionState(id, ExecutionStatus.RUNNING, new EntityId("n1"),
            Map.of(), Set.of(new EntityId("n1")), Set.of(), 1, 2);
        var event = new ExecutionEvent(id, 1, Instant.now(), "NODE_STARTED", Map.of());
        var activation = ActivationChange.started("act-1", new EntityId("n1"));
        var dispatch = new DispatchIntent("disp-1", new EntityId("n2"), "act-1", 1, Map.of("input", "value"));

        var command = new CommitPort.CommitCommand(
            newState, event, 0, null,
            List.of(activation), List.of(), List.of(dispatch),
            Map.of("outbox-1", "dispatch-ready"));

        assertTrue(port.commit(command));

        // 事件已发布
        assertEquals(1, publisher.count());
        // 激活变更已存储
        assertEquals(1, port.activations(id).size());
        assertEquals(ActivationChange.ActivationStatus.STARTED, port.activations(id).get(0).status());
        // 派发意图已存储
        assertEquals(1, port.dispatches(id).size());
        assertEquals("disp-1", port.dispatches(id).get(0).dispatchId());
        // Outbox 已存储
        assertEquals("dispatch-ready", port.outbox(id).get("outbox-1"));
    }

    @Test
    void additionalEventsArePublishedInSameAtomicUnit() {
        var publisher = new CollectingPublisher();
        var port = new InMemoryCommitPort(publisher);
        var id = new ExecutionId("atomic-2");
        port.create(new ExecutionState(id, ExecutionStatus.CREATED, null, Map.of(), Set.of(), Set.of(), 0));

        var newState = new ExecutionState(id, ExecutionStatus.RUNNING, new EntityId("n1"),
            Map.of(), Set.of(), Set.of(), 1, 3);
        var event = new ExecutionEvent(id, 1, Instant.now(), "E1", Map.of());
        var extra1 = new ExecutionEvent(id, 2, Instant.now(), "E2", Map.of());
        var extra2 = new ExecutionEvent(id, 3, Instant.now(), "E3", Map.of());

        var command = new CommitPort.CommitCommand(
            newState, event, 0, null,
            List.of(), List.of(extra1, extra2), List.of(), Map.of());

        assertTrue(port.commit(command));
        assertEquals(3, publisher.count());
    }

    @Test
    void casContentionOnlyOneExtendedCommitSucceeds() throws Exception {
        var publisher = new CollectingPublisher();
        var port = new InMemoryCommitPort(publisher);
        var id = new ExecutionId("cas-extended");
        port.create(new ExecutionState(id, ExecutionStatus.READY, new EntityId("n1"),
            Map.of(), Set.of(), Set.of(), 0, 0));

        var latch = new CountDownLatch(1);
        var successCount = new AtomicInteger(0);

        for (int i = 0; i < 5; i++) {
            new Thread(() -> {
                try { latch.await(); } catch (InterruptedException e) { return; }
                var newState = new ExecutionState(id, ExecutionStatus.RUNNING, new EntityId("n1"),
                    Map.of("thread", Thread.currentThread().getName()),
                    Set.of(new EntityId("n1")), Set.of(), 1, 1);
                var event = new ExecutionEvent(id, 1, Instant.now(), "RACE",
                    Map.of("thread", Thread.currentThread().getName()));
                var command = new CommitPort.CommitCommand(newState, event, 0);
                if (port.commit(command)) successCount.incrementAndGet();
            }).start();
        }

        latch.countDown();
        Thread.sleep(200);
        assertEquals(1, successCount.get(), "Exactly one CAS should succeed");
    }

    @Test
    void backwardCompatibleThreeArgConstructorStillWorks() {
        var publisher = new CollectingPublisher();
        var port = new InMemoryCommitPort(publisher);
        var id = new ExecutionId("compat-1");
        port.create(new ExecutionState(id, ExecutionStatus.CREATED, null, Map.of(), Set.of(), Set.of(), 0));

        var newState = new ExecutionState(id, ExecutionStatus.RUNNING, new EntityId("n1"),
            Map.of(), Set.of(), Set.of(), 1);
        var event = new ExecutionEvent(id, 1, Instant.now(), "STARTED", Map.of());

        // 使用旧的 3 参数构造函数
        assertTrue(port.commit(new CommitPort.CommitCommand(newState, event, 0)));
        assertEquals(1, publisher.count());
        // 扩展字段应为空
        assertTrue(port.activations(id).isEmpty());
        assertTrue(port.dispatches(id).isEmpty());
        assertTrue(port.outbox(id).isEmpty());
    }

    @Test
    void leaseFenceValidation() {
        assertThrows(IllegalArgumentException.class,
            () -> new CommitPort.LeaseFence("", 1));
        assertThrows(IllegalArgumentException.class,
            () -> new CommitPort.LeaseFence("worker-1", 0));
        assertThrows(IllegalArgumentException.class,
            () -> new CommitPort.LeaseFence("worker-1", -1));
        var fence = new CommitPort.LeaseFence("worker-1", 42);
        assertEquals("worker-1", fence.workerId());
        assertEquals(42, fence.fencingToken());
    }
}
