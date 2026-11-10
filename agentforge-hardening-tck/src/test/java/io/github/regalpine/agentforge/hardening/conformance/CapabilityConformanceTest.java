package io.github.regalpine.agentforge.hardening.conformance;

import io.github.regalpine.agentforge.capability.*;
import io.github.regalpine.agentforge.hardening.*;
import io.github.regalpine.agentforge.kernel.*;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * conformance-capability: Registry
 * / Resolution / Invocation / Policy.
 * AF-RELEASE-02 §4, T-H6.2.
 */
class CapabilityConformanceTest {

    private CapabilityDefinition makeCap(String id) {
        return new CapabilityDefinition(
            new EntityId(id),
            new Version("1.0.0"),
            id,
            null, null, null
        );
    }

    @Test
    void afCap001_registerAndFind() {
        var registry = new InMemoryCapabilityRegistry();
        var cap = makeCap("cap.test");
        registry.register(cap);

        var ref = new Reference(
            new EntityId("cap.test"),
            new Version("1.0.0")
        );
        var found = registry.find(ref);
        assertTrue(found.isPresent());
        assertEquals("cap.test", found.get().name());
    }

    @Test
    void afCap002_duplicateRegistrationThrows() {
        var registry = new InMemoryCapabilityRegistry();
        var cap = makeCap("cap.dup");
        registry.register(cap);
        assertThrows(
            IllegalStateException.class,
            () -> registry.register(cap)
        );
    }

    @Test
    void afCap003_unregister() {
        var registry = new InMemoryCapabilityRegistry();
        var cap = makeCap("cap.rm");
        registry.register(cap);
        var ref = new Reference(
            new EntityId("cap.rm"),
            new Version("1.0.0")
        );
        registry.unregister(ref);
        assertTrue(registry.find(ref).isEmpty());
    }

    @Test
    void afCap004_listAll() {
        var registry = new InMemoryCapabilityRegistry();
        for (int i = 0; i < 5; i++) {
            registry.register(makeCap("cap.list." + i));
        }
        assertEquals(5, registry.list().size());
    }

    @Test
    void afCap005_findMissingReturnsEmpty() {
        var registry = new InMemoryCapabilityRegistry();
        var ref = new Reference(
            new EntityId("cap.missing"),
            new Version("1.0.0")
        );
        assertTrue(registry.find(ref).isEmpty());
    }
}
