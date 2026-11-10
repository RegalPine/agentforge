package io.github.regalpine.agentforge.runtime.streaming;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class InMemoryEventBusTest {

    @Test
    void shouldPublishAndReceiveEvents() {
        InMemoryEventBus bus = new InMemoryEventBus();
        List<AgentForgeEvent> received = new ArrayList<>();

        bus.subscribe(received::add);
        bus.publish(AgentForgeEvent.execution(EventType.EXECUTION_STARTED, "exec-1"));

        assertEquals(1, received.size());
        assertEquals(EventType.EXECUTION_STARTED, received.get(0).type());
    }

    @Test
    void shouldFilterByEventType() {
        InMemoryEventBus bus = new InMemoryEventBus();
        List<AgentForgeEvent> received = new ArrayList<>();

        bus.subscribe(EventType.NODE_STARTED, received::add);
        bus.publish(AgentForgeEvent.execution(EventType.EXECUTION_STARTED, "exec-1"));
        bus.publish(AgentForgeEvent.node(EventType.NODE_STARTED, "exec-1", "node-1"));

        assertEquals(1, received.size());
        assertEquals(EventType.NODE_STARTED, received.get(0).type());
    }

    @Test
    void shouldAssignSequenceNumbers() {
        InMemoryEventBus bus = new InMemoryEventBus();
        List<AgentForgeEvent> received = new ArrayList<>();

        bus.subscribe(received::add);
        long seq1 = bus.publishWithSequence(AgentForgeEvent.execution(EventType.EXECUTION_STARTED, "exec-1"));
        long seq2 = bus.publishWithSequence(AgentForgeEvent.execution(EventType.EXECUTION_COMPLETED, "exec-1"));

        assertEquals(1, seq1);
        assertEquals(2, seq2);
        assertEquals(1, received.get(0).sequenceNumber());
        assertEquals(2, received.get(1).sequenceNumber());
    }

    @Test
    void shouldCancelSubscription() {
        InMemoryEventBus bus = new InMemoryEventBus();
        AtomicInteger count = new AtomicInteger(0);

        EventStream.Subscription sub = bus.subscribe(e -> count.incrementAndGet());
        bus.publish(AgentForgeEvent.execution(EventType.EXECUTION_STARTED, "exec-1"));
        assertEquals(1, count.get());

        sub.cancel();
        bus.publish(AgentForgeEvent.execution(EventType.EXECUTION_COMPLETED, "exec-1"));
        assertEquals(1, count.get()); // no more events
    }

    @Test
    void shouldCloseBus() {
        InMemoryEventBus bus = new InMemoryEventBus();
        bus.subscribe(e -> {});
        assertEquals(1, bus.subscriptionCount());

        bus.close();
        assertTrue(bus.isClosed());
        assertEquals(0, bus.subscriptionCount());
    }

    @Test
    void shouldRejectSubscribeAfterClose() {
        InMemoryEventBus bus = new InMemoryEventBus();
        bus.close();

        assertThrows(IllegalStateException.class, () -> bus.subscribe(e -> {}));
    }

    @Test
    void shouldIgnorePublishAfterClose() {
        InMemoryEventBus bus = new InMemoryEventBus();
        bus.close();

        // Should not throw
        bus.publish(AgentForgeEvent.execution(EventType.EXECUTION_STARTED, "exec-1"));
    }

    @Test
    void shouldHandleMultipleSubscribers() {
        InMemoryEventBus bus = new InMemoryEventBus();
        AtomicInteger count1 = new AtomicInteger(0);
        AtomicInteger count2 = new AtomicInteger(0);

        bus.subscribe(e -> count1.incrementAndGet());
        bus.subscribe(e -> count2.incrementAndGet());
        bus.publish(AgentForgeEvent.execution(EventType.EXECUTION_STARTED, "exec-1"));

        assertEquals(1, count1.get());
        assertEquals(1, count2.get());
    }

    @Test
    void shouldCreateEventsWithFactoryMethods() {
        AgentForgeEvent exec = AgentForgeEvent.execution(EventType.EXECUTION_STARTED, "exec-1");
        assertEquals("exec-1", exec.executionId());
        assertNull(exec.nodeId());
        assertNull(exec.agentId());

        AgentForgeEvent node = AgentForgeEvent.node(EventType.NODE_STARTED, "exec-1", "node-1");
        assertEquals("node-1", node.nodeId());

        AgentForgeEvent agent = AgentForgeEvent.agent(EventType.AGENT_LOOP_STARTED, "exec-1", "agent-1");
        assertEquals("agent-1", agent.agentId());

        AgentForgeEvent model = AgentForgeEvent.model(EventType.MODEL_REQUEST_STARTED, "exec-1", Map.of("model", "gpt-4"));
        assertEquals("gpt-4", model.payload().get("model"));
    }

    @Test
    void shouldCreateCopyWithSequence() {
        AgentForgeEvent event = AgentForgeEvent.execution(EventType.EXECUTION_STARTED, "exec-1");
        AgentForgeEvent sequenced = event.withSequence(42);

        assertEquals(42, sequenced.sequenceNumber());
        assertEquals(event.type(), sequenced.type());
        assertEquals(event.executionId(), sequenced.executionId());
    }
}
