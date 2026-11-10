package io.github.regalpine.agentforge.server.agent;

import io.github.regalpine.agentforge.agent.AgentDefinition;
import io.github.regalpine.agentforge.agent.AgentHealth;
import io.github.regalpine.agentforge.agent.AgentLifecycleState;
import io.github.regalpine.agentforge.agent.AgentMetrics;
import io.github.regalpine.agentforge.agent.InMemoryAgentRegistrar;
import io.github.regalpine.agentforge.agent.RegistrationHandle;
import io.github.regalpine.agentforge.agent.RegistrationOptions;
import io.github.regalpine.agentforge.event.EventPublisher;
import io.github.regalpine.agentforge.event.ExecutionEvent;
import io.github.regalpine.agentforge.event.InMemoryEventPublisher;
import io.github.regalpine.agentforge.kernel.EntityId;
import io.github.regalpine.agentforge.kernel.Reference;
import io.github.regalpine.agentforge.kernel.Version;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for
 * {@link AgentForgeAgentRegistrar}.
 *
 * <p>Verifies that lifecycle operations
 * publish the expected events and that
 * the decorator correctly delegates to
 * {@link InMemoryAgentRegistrar}.
 */
class AgentForgeAgentRegistrarTest {

    private InMemoryEventPublisher events;
    private AgentForgeAgentRegistrar registrar;

    @BeforeEach
    void setUp() {
        events = new InMemoryEventPublisher();
        registrar = new AgentForgeAgentRegistrar(events);
    }

    private AgentDefinition def(String id, String ver) {
        return new AgentDefinition(
            new EntityId(id),
            new Version(ver),
            "agent-" + id,
            Set.of()
        );
    }

    private Reference ref(String id, String ver) {
        return new Reference(
            new EntityId(id), new Version(ver)
        );
    }

    // ── Registration ──

    @Test
    void shouldPublishRegisteredEvent() {
        registrar.register(def("a1", "1.0.0"));
        assertEquals(1, events.events().size());
        assertEquals("AGENT_REGISTERED",
            events.events().get(0).type());
    }

    @Test
    void shouldPublishRegisterAndStartEvents() {
        registrar.registerAndStart(
            def("a2", "1.0.0"),
            RegistrationOptions.defaults()
        );
        // AGENT_REGISTERED + AGENT_STARTED
        assertEquals(2, events.events().size());
        assertEquals("AGENT_REGISTERED",
            events.events().get(0).type());
        assertEquals("AGENT_STARTED",
            events.events().get(1).type());
    }

    // ── Lifecycle ──

    @Test
    void shouldPublishStartStopEvents() {
        Reference r = ref("a3", "1.0.0");
        registrar.register(def("a3", "1.0.0"));
        int before = events.events().size();

        registrar.start(r);
        assertEquals(before + 1, events.events().size());
        assertEquals("AGENT_STARTED",
            events.events().get(before).type());

        registrar.stop(r);
        assertEquals(before + 2, events.events().size());
        assertEquals("AGENT_STOPPED",
            events.events().get(before + 1).type());
    }

    @Test
    void shouldPublishDrainEvent() {
        Reference r = ref("a4", "1.0.0");
        registrar.registerAndStart(
            def("a4", "1.0.0"),
            RegistrationOptions.defaults()
        );
        int before = events.events().size();

        registrar.drain(r);
        assertEquals(before + 1, events.events().size());
        assertEquals("AGENT_DRAINED",
            events.events().get(before).type());
    }

    @Test
    void shouldPublishUnregisterEvent() {
        Reference r = ref("a5", "1.0.0");
        registrar.register(def("a5", "1.0.0"));
        int before = events.events().size();

        registrar.unregister(r);
        assertEquals(before + 1, events.events().size());
        assertEquals("AGENT_UNREGISTERED",
            events.events().get(before).type());
    }

    // ── Delegation ──

    @Test
    void shouldDelegateLifecycleState() {
        Reference r = ref("a6", "1.0.0");
        registrar.register(
            def("a6", "1.0.0"),
            RegistrationOptions.of(false));
        assertEquals(AgentLifecycleState.REGISTERED,
            registrar.lifecycleState(r));

        registrar.start(r);
        assertEquals(AgentLifecycleState.ACTIVE,
            registrar.lifecycleState(r));
    }

    @Test
    void shouldDelegateHealth() {
        Reference r = ref("a7", "1.0.0");
        registrar.register(def("a7", "1.0.0"));
        AgentHealth h = registrar.health(r);
        assertNotNull(h);
        assertEquals(AgentHealth.AgentHealthStatus.UP,
            h.status());
    }

    @Test
    void shouldDelegateMetrics() {
        Reference r = ref("a8", "1.0.0");
        registrar.register(def("a8", "1.0.0"));
        AgentMetrics m = registrar.metrics(r);
        assertNotNull(m);
        assertEquals(0, m.invocations());
    }

    @Test
    void shouldDelegateList() {
        registrar.register(def("a9", "1.0.0"));
        registrar.register(def("a10", "1.0.0"));
        assertEquals(2, registrar.list().size());
    }

    // ── Negative ──

    @Test
    void shouldNotBreakOnEventPublishFailure() {
        // Use a publisher that throws
        EventPublisher broken = event ->
            { throw new RuntimeException("boom"); };
        var reg = new AgentForgeAgentRegistrar(broken);
        // Should not throw
        assertDoesNotThrow(() ->
            reg.register(def("a11", "1.0.0")));
    }

    @Test
    void shouldRejectNullEventPublisher() {
        assertThrows(NullPointerException.class, () ->
            new AgentForgeAgentRegistrar(null));
    }
}
