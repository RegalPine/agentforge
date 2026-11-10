package io.github.regalpine.agentforge.kernel;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ReferenceTest {

    @Test
    void shouldCreateReference() {
        Reference ref = Reference.of("calculator.evaluate", "1.0.0");

        assertEquals("calculator.evaluate", ref.id().value());
        assertEquals("1.0.0", ref.version().value());
    }

    @Test
    void shouldRejectNullId() {
        assertThrows(
            NullPointerException.class,
            () -> new Reference(null, new Version("1.0.0"))
        );
    }

    @Test
    void shouldRejectNullVersion() {
        assertThrows(
            NullPointerException.class,
            () -> new Reference(new EntityId("test"), null)
        );
    }

    @Test
    void shouldSupportEquality() {
        Reference a = Reference.of("test", "1.0.0");
        Reference b = Reference.of("test", "1.0.0");
        assertEquals(a, b);
    }
}
