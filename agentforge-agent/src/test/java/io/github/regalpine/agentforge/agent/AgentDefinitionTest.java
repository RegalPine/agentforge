package io.github.regalpine.agentforge.agent;

import io.github.regalpine.agentforge.kernel.EntityId;
import io.github.regalpine.agentforge.kernel.Reference;
import io.github.regalpine.agentforge.kernel.Version;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class AgentDefinitionTest {

    private static final EntityId ID =
        new EntityId("agent.test");

    private static final Version VERSION =
        new Version("1.0.0");

    @Test
    void shouldCreateWithAllFields() {
        Set<Reference> caps = Set.of(
            Reference.of("cap.translate", "1.0.0"),
            Reference.of("cap.summarize", "1.0.0")
        );

        AgentDefinition def = new AgentDefinition(
            ID, VERSION, "translator", caps
        );

        assertEquals(ID, def.id());
        assertEquals(VERSION, def.version());
        assertEquals("translator", def.name());
        assertEquals(2, def.capabilities().size());
    }

    @Test
    void shouldImplementAgentInterface() {
        AgentDefinition def = new AgentDefinition(
            ID, VERSION, "agent", Set.of()
        );

        Agent agent = def;
        assertEquals("agent", agent.name());
        assertEquals(ID, agent.id());
        assertEquals(VERSION, agent.version());
    }

    @Test
    void shouldRejectBlankName() {
        assertThrows(
            IllegalArgumentException.class,
            () -> new AgentDefinition(
                ID, VERSION, "", Set.of()
            )
        );
    }

    @Test
    void shouldRejectNullName() {
        assertThrows(
            IllegalArgumentException.class,
            () -> new AgentDefinition(
                ID, VERSION, null, Set.of()
            )
        );
    }

    @Test
    void shouldRejectWhitespaceName() {
        assertThrows(
            IllegalArgumentException.class,
            () -> new AgentDefinition(
                ID, VERSION, "   ", Set.of()
            )
        );
    }

    @Test
    void shouldDefaultNullCapabilitiesToEmptySet() {
        AgentDefinition def = new AgentDefinition(
            ID, VERSION, "agent", null
        );

        assertNotNull(def.capabilities());
        assertTrue(def.capabilities().isEmpty());
    }

    @Test
    void shouldDefensivelyCopyCapabilities() {
        Set<Reference> original = new HashSet<>();
        original.add(Reference.of("cap.x", "1.0.0"));

        AgentDefinition def = new AgentDefinition(
            ID, VERSION, "agent", original
        );

        // Mutating original must not affect the record
        original.add(Reference.of("cap.y", "1.0.0"));
        assertEquals(1, def.capabilities().size());
    }

    @Test
    void shouldReturnUnmodifiableCapabilities() {
        AgentDefinition def = new AgentDefinition(
            ID, VERSION, "agent",
            Set.of(Reference.of("cap.x", "1.0.0"))
        );

        assertThrows(
            UnsupportedOperationException.class,
            () -> def.capabilities().add(
                Reference.of("cap.y", "1.0.0")
            )
        );
    }

    @Test
    void shouldSupportEquality() {
        Set<Reference> caps = Set.of(
            Reference.of("cap.a", "1.0.0")
        );

        AgentDefinition a = new AgentDefinition(
            ID, VERSION, "agent", caps
        );
        AgentDefinition b = new AgentDefinition(
            ID, VERSION, "agent", caps
        );

        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
    }

    @Test
    void shouldDifferByName() {
        AgentDefinition a = new AgentDefinition(
            ID, VERSION, "alpha", Set.of()
        );
        AgentDefinition b = new AgentDefinition(
            ID, VERSION, "beta", Set.of()
        );

        assertNotEquals(a, b);
    }

    @Test
    void shouldDifferByIdentity() {
        AgentDefinition a = new AgentDefinition(
            new EntityId("agent.one"), VERSION,
            "agent", Set.of()
        );
        AgentDefinition b = new AgentDefinition(
            new EntityId("agent.two"), VERSION,
            "agent", Set.of()
        );

        assertNotEquals(a, b);
    }

    @Test
    void shouldAcceptEmptyCapabilities() {
        AgentDefinition def = new AgentDefinition(
            ID, VERSION, "lonely", Set.of()
        );

        assertTrue(def.capabilities().isEmpty());
    }

    @Test
    void shouldAcceptMultipleCapabilities() {
        Set<Reference> caps = Set.of(
            Reference.of("cap.a", "1.0.0"),
            Reference.of("cap.b", "2.0.0"),
            Reference.of("cap.c", "3.0.0")
        );

        AgentDefinition def = new AgentDefinition(
            ID, VERSION, "multi", caps
        );

        assertEquals(3, def.capabilities().size());
    }
}
