package io.github.regalpine.agentforge.kernel;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class EntityIdTest {

    @Test
    void shouldCreateValidId() {
        EntityId id = new EntityId("calculator.evaluate");

        assertEquals(
            "calculator.evaluate",
            id.value()
        );
    }

    @Test
    void shouldRejectBlankId() {
        assertThrows(
            IllegalArgumentException.class,
            () -> new EntityId(" ")
        );
    }

    @Test
    void shouldRejectInvalidId() {
        assertThrows(
            IllegalArgumentException.class,
            () -> new EntityId("invalid id")
        );
    }

    @Test
    void shouldRejectNullId() {
        assertThrows(
            NullPointerException.class,
            () -> new EntityId(null)
        );
    }

    @Test
    void shouldAcceptDotsAndDashes() {
        EntityId id = new EntityId("my-agent.v2:test-1");
        assertEquals("my-agent.v2:test-1", id.value());
    }

    @Test
    void shouldSupportEquality() {
        EntityId a = new EntityId("test");
        EntityId b = new EntityId("test");
        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
    }

    @Test
    void shouldReturnStringValue() {
        EntityId id = new EntityId("hello.world");
        assertEquals("hello.world", id.toString());
    }
}
