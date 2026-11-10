package io.github.regalpine.agentforge.kernel;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class VersionTest {

    @Test
    void shouldCreateValidVersion() {
        Version v = new Version("1.0.0");
        assertEquals("1.0.0", v.value());
    }

    @Test
    void shouldRejectInvalidFormat() {
        assertThrows(
            IllegalArgumentException.class,
            () -> new Version("1.0")
        );
    }

    @Test
    void shouldRejectNull() {
        assertThrows(
            NullPointerException.class,
            () -> new Version(null)
        );
    }

    @Test
    void shouldRejectNonNumeric() {
        assertThrows(
            IllegalArgumentException.class,
            () -> new Version("a.b.c")
        );
    }

    @Test
    void shouldSupportEquality() {
        Version a = new Version("2.1.0");
        Version b = new Version("2.1.0");
        assertEquals(a, b);
    }
}
