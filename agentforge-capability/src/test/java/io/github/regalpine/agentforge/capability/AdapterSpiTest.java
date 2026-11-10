package io.github.regalpine.agentforge.capability;

import io.github.regalpine.agentforge.kernel.EntityId;
import io.github.regalpine.agentforge.kernel.Version;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for H1 Adapter SPI:
 * AdapterMetadata, AdapterHealth,
 * AdapterConfiguration, CapabilityAdapter,
 * AdapterRegistry.
 */
class AdapterSpiTest {

    // --- AdapterMetadata ---

    @Test
    void shouldCreateAdapterMetadata() {
        var meta = new AdapterMetadata(
            "http-1",
            "http",
            "1.0.0",
            "HTTP Adapter",
            "REST endpoint adapter",
            Set.of(
                SideEffectClassification.READ_ONLY,
                SideEffectClassification.WRITE
            ),
            Map.of(),
            Map.of("env", "test")
        );

        assertEquals("http-1", meta.adapterId());
        assertEquals("http", meta.protocol());
        assertEquals(2, meta.supportedSideEffects().size());
        assertEquals("test", meta.metadata().get("env"));
    }

    @Test
    void shouldRejectBlankAdapterId() {
        assertThrows(
            IllegalArgumentException.class,
            () -> new AdapterMetadata(
                "", "http", "1.0.0",
                null, null, null, null, null
            )
        );
    }

    @Test
    void shouldDefaultSupportedSideEffects() {
        var meta = new AdapterMetadata(
            "test-1", "test", "1.0.0",
            null, null, null, null, null
        );

        assertEquals(
            1,
            meta.supportedSideEffects().size()
        );

        assertTrue(
            meta.supportedSideEffects().contains(
                SideEffectClassification.READ_ONLY
            )
        );
    }

    // --- AdapterHealth ---

    @Test
    void shouldCreateAvailableHealth() {
        var health = AdapterHealth.available();

        assertEquals(
            AdapterHealthStatus.AVAILABLE,
            health.status()
        );

        assertNotNull(health.checkedAt());
    }

    @Test
    void shouldCreateUnavailableHealth() {
        var health =
            AdapterHealth.unavailable("down");

        assertEquals(
            AdapterHealthStatus.UNAVAILABLE,
            health.status()
        );

        assertEquals("down", health.message());
    }

    @Test
    void shouldCreateUnknownHealth() {
        var health = AdapterHealth.unknown();

        assertEquals(
            AdapterHealthStatus.UNKNOWN,
            health.status()
        );
    }

    // --- AdapterHealthStatus ---

    @Test
    void shouldHaveAllHealthStatuses() {
        assertEquals(
            4,
            AdapterHealthStatus.values().length
        );
    }

    // --- AdapterConfiguration ---

    @Test
    void shouldRequireProperty() {
        var config = new AdapterConfiguration(
            "test",
            Map.of("host", "localhost")
        );

        assertEquals(
            "localhost",
            config.require("host")
        );
    }

    @Test
    void shouldThrowOnMissingRequired() {
        var config = new AdapterConfiguration(
            "test", Map.of()
        );

        assertThrows(
            IllegalArgumentException.class,
            () -> config.require("missing")
        );
    }

    @Test
    void shouldGetOrDefault() {
        var config = new AdapterConfiguration(
            "test",
            Map.of("port", "8080")
        );

        assertEquals(
            "8080",
            config.getOrDefault("port", "3000")
        );

        assertEquals(
            "3000",
            config.getOrDefault(
                "missing", "3000"
            )
        );
    }

    @Test
    void shouldGetIntProperty() {
        var config = new AdapterConfiguration(
            "test",
            Map.of("timeout", 5000)
        );

        assertEquals(
            5000,
            config.getInt("timeout", 1000)
        );

        assertEquals(
            1000,
            config.getInt("missing", 1000)
        );
    }

    @Test
    void shouldGetBooleanProperty() {
        var config = new AdapterConfiguration(
            "test",
            Map.of("ssl", true)
        );

        assertTrue(
            config.getBoolean("ssl", false)
        );

        assertFalse(
            config.getBoolean("missing", false)
        );
    }

    // --- InMemoryAdapterRegistry ---

    @Test
    void shouldRegisterAndFindAdapter() {
        var registry =
            new InMemoryAdapterRegistry();

        var adapter = new StubAdapter("test-1");

        registry.register(adapter);

        var found = registry.find("test-1");

        assertTrue(found.isPresent());
        assertEquals(
            "test-1",
            found.get().metadata().adapterId()
        );
    }

    @Test
    void shouldRejectDuplicateRegistration() {
        var registry =
            new InMemoryAdapterRegistry();

        registry.register(
            new StubAdapter("dup")
        );

        assertThrows(
            IllegalStateException.class,
            () -> registry.register(
                new StubAdapter("dup")
            )
        );
    }

    @Test
    void shouldFindByProtocol() {
        var registry =
            new InMemoryAdapterRegistry();

        registry.register(
            new StubAdapter("http-1", "http")
        );

        registry.register(
            new StubAdapter("http-2", "http")
        );

        registry.register(
            new StubAdapter("jdbc-1", "jdbc")
        );

        var httpAdapters =
            registry.findByProtocol("http");

        assertEquals(2, httpAdapters.size());

        var jdbcAdapters =
            registry.findByProtocol("jdbc");

        assertEquals(1, jdbcAdapters.size());
    }

    @Test
    void shouldListAllAdapters() {
        var registry =
            new InMemoryAdapterRegistry();

        registry.register(
            new StubAdapter("a1")
        );

        registry.register(
            new StubAdapter("a2")
        );

        assertEquals(
            2,
            registry.list().size()
        );
    }

    @Test
    void shouldUnregisterAdapter() {
        var registry =
            new InMemoryAdapterRegistry();

        registry.register(
            new StubAdapter("rm-1")
        );

        assertEquals(1, registry.list().size());

        registry.unregister("rm-1");

        assertEquals(0, registry.list().size());
    }

    @Test
    void shouldProduceHealthSummary() {
        var registry =
            new InMemoryAdapterRegistry();

        registry.register(
            new StubAdapter("h1")
        );

        var summary = registry.healthSummary();

        assertEquals(1, summary.size());
        assertTrue(summary.containsKey("h1"));
    }

    @Test
    void shouldCloseAllAdapters() {
        var registry =
            new InMemoryAdapterRegistry();

        registry.register(
            new StubAdapter("c1")
        );

        registry.register(
            new StubAdapter("c2")
        );

        assertEquals(2, registry.list().size());

        registry.closeAll();

        assertEquals(0, registry.list().size());
    }

    // --- Stub adapter ---

    private static class StubAdapter
        implements CapabilityAdapter {

        private final String id;
        private final String protocol;

        StubAdapter(String id) {
            this(id, "test");
        }

        StubAdapter(String id, String protocol) {
            this.id = id;
            this.protocol = protocol;
        }

        @Override
        public AdapterMetadata metadata() {
            return new AdapterMetadata(
                id, protocol, "1.0.0",
                null, null, null, null, null
            );
        }

        @Override
        public CapabilityResult execute(
            CapabilityInvocation invocation
        ) {
            return new CapabilitySuccess(
                Map.of("adapter", id)
            );
        }

        @Override
        public AdapterHealth health() {
            return AdapterHealth.available();
        }
    }
}
