package io.github.regalpine.agentforge.provider.spi;

import io.github.regalpine.agentforge.capability.CapabilityProvider;
import io.github.regalpine.agentforge.provider.spi.registry.DefaultProviderRegistry;
import io.github.regalpine.agentforge.provider.spi.registry.ProviderRegistry;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * BE-06: Tests for ServiceLoader-based auto-discovery.
 *
 * When no META-INF/services file is on the classpath,
 * autoDiscover should return 0 without errors.
 */
class ProviderAutoDiscoverTest {

    @Test
    void shouldAutoDiscoverZeroWhenNoServiceFile() {
        ProviderRegistry registry =
            new DefaultProviderRegistry();

        int count = registry.autoDiscover();

        // No META-INF/services on test classpath
        assertEquals(0, count);
    }

    @Test
    void shouldNotThrowOnAutoDiscover() {
        ProviderRegistry registry =
            new DefaultProviderRegistry();

        assertDoesNotThrow(registry::autoDiscover);
    }

    @Test
    void shouldReturnAllIncludingAutoDiscovered() {
        ProviderRegistry registry =
            new DefaultProviderRegistry();

        registry.autoDiscover();

        // all() should still work after autoDiscover
        assertNotNull(registry.all());
    }
}
