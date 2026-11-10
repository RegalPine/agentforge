package io.github.regalpine.agentforge.agent;

import io.github.regalpine.agentforge.kernel.EntityId;
import io.github.regalpine.agentforge.kernel.Reference;
import io.github.regalpine.agentforge.kernel.Version;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class InMemoryAgentRegistryTest {

    private InMemoryAgentRegistry registry;

    @BeforeEach
    void setUp() {
        registry = new InMemoryAgentRegistry();
    }

    private AgentDefinition agent(
        String id, String version, String name
    ) {
        return new AgentDefinition(
            new EntityId(id),
            new Version(version),
            name,
            Set.of()
        );
    }

    private AgentDefinition agentWithCaps(
        String id, String version, String name,
        Set<Reference> caps
    ) {
        return new AgentDefinition(
            new EntityId(id),
            new Version(version),
            name,
            caps
        );
    }

    @Test
    void shouldRegisterAndFind() {
        AgentDefinition def = agent(
            "agent.one", "1.0.0", "alpha"
        );

        registry.register(def);

        Reference ref = new Reference(
            new EntityId("agent.one"),
            new Version("1.0.0")
        );

        var found = registry.find(ref);
        assertTrue(found.isPresent());
        assertEquals(def, found.get());
    }

    @Test
    void shouldReturnEmptyForUnknownReference() {
        Reference ref = Reference.of("unknown", "1.0.0");

        var found = registry.find(ref);
        assertFalse(found.isPresent());
    }

    @Test
    void shouldRejectDuplicateRegistration() {
        AgentDefinition def = agent(
            "agent.dup", "1.0.0", "dup"
        );

        registry.register(def);

        assertThrows(
            IllegalStateException.class,
            () -> registry.register(def)
        );
    }

    @Test
    void shouldAllowSameIdDifferentVersion() {
        AgentDefinition v1 = agent(
            "agent.multi", "1.0.0", "v1"
        );
        AgentDefinition v2 = agent(
            "agent.multi", "2.0.0", "v2"
        );

        registry.register(v1);
        registry.register(v2);

        Reference ref1 = new Reference(
            new EntityId("agent.multi"),
            new Version("1.0.0")
        );
        Reference ref2 = new Reference(
            new EntityId("agent.multi"),
            new Version("2.0.0")
        );

        assertEquals(v1, registry.find(ref1).orElseThrow());
        assertEquals(v2, registry.find(ref2).orElseThrow());
    }

    @Test
    void shouldListAllRegistered() {
        AgentDefinition a = agent("agent.a", "1.0.0", "a");
        AgentDefinition b = agent("agent.b", "1.0.0", "b");
        AgentDefinition c = agent("agent.c", "1.0.0", "c");

        registry.register(a);
        registry.register(b);
        registry.register(c);

        List<AgentDefinition> list = registry.list();
        assertEquals(3, list.size());
        assertTrue(list.contains(a));
        assertTrue(list.contains(b));
        assertTrue(list.contains(c));
    }

    @Test
    void shouldReturnEmptyListWhenNoAgents() {
        List<AgentDefinition> list = registry.list();
        assertNotNull(list);
        assertTrue(list.isEmpty());
    }

    @Test
    void shouldReturnDefensiveCopyFromList() {
        registry.register(
            agent("agent.x", "1.0.0", "x")
        );

        List<AgentDefinition> list = registry.list();

        assertThrows(
            UnsupportedOperationException.class,
            () -> list.add(
                agent("agent.y", "1.0.0", "y")
            )
        );
    }

    @Test
    void shouldUnregisterExistingAgent() {
        AgentDefinition def = agent(
            "agent.rm", "1.0.0", "removable"
        );

        registry.register(def);

        Reference ref = new Reference(
            new EntityId("agent.rm"),
            new Version("1.0.0")
        );

        registry.unregister(ref);

        assertFalse(registry.find(ref).isPresent());
    }

    @Test
    void shouldTolerateUnregisterOfUnknown() {
        Reference ref = Reference.of("ghost", "1.0.0");

        assertDoesNotThrow(
            () -> registry.unregister(ref)
        );
    }

    @Test
    void shouldAllowReregisterAfterUnregister() {
        AgentDefinition def = agent(
            "agent.recycle", "1.0.0", "recycle"
        );

        registry.register(def);

        Reference ref = new Reference(
            new EntityId("agent.recycle"),
            new Version("1.0.0")
        );

        registry.unregister(ref);

        // Should not throw after removal
        registry.register(def);
        assertTrue(registry.find(ref).isPresent());
    }

    @Test
    void shouldPreserveCapabilities() {
        Set<Reference> caps = Set.of(
            Reference.of("cap.translate", "1.0.0"),
            Reference.of("cap.summarize", "2.0.0")
        );

        AgentDefinition def = agentWithCaps(
            "agent.cap", "1.0.0", "capable", caps
        );

        registry.register(def);

        Reference ref = new Reference(
            new EntityId("agent.cap"),
            new Version("1.0.0")
        );

        AgentDefinition found = registry.find(ref).orElseThrow();
        assertEquals(2, found.capabilities().size());
        assertTrue(found.capabilities().containsAll(caps));
    }

    @Test
    void shouldIsolateAfterUnregister() {
        AgentDefinition a = agent("agent.a", "1.0.0", "a");
        AgentDefinition b = agent("agent.b", "1.0.0", "b");

        registry.register(a);
        registry.register(b);

        Reference refA = new Reference(
            new EntityId("agent.a"),
            new Version("1.0.0")
        );

        registry.unregister(refA);

        assertEquals(1, registry.list().size());
        assertFalse(registry.find(refA).isPresent());
        assertTrue(registry.find(
            new Reference(
                new EntityId("agent.b"),
                new Version("1.0.0")
            )
        ).isPresent());
    }

    @Test
    void shouldListAfterMixedOperations() {
        AgentDefinition a = agent("agent.a", "1.0.0", "a");
        AgentDefinition b = agent("agent.b", "1.0.0", "b");
        AgentDefinition c = agent("agent.c", "1.0.0", "c");

        registry.register(a);
        registry.register(b);
        registry.register(c);

        registry.unregister(new Reference(
            new EntityId("agent.b"),
            new Version("1.0.0")
        ));

        List<AgentDefinition> list = registry.list();
        assertEquals(2, list.size());
        assertTrue(list.contains(a));
        assertFalse(list.contains(b));
        assertTrue(list.contains(c));
    }
}
