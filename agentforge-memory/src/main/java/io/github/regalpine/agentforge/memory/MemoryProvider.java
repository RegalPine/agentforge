package io.github.regalpine.agentforge.memory;

import java.util.Collection;
import java.util.Map;

/**
 * Memory provider SPI.
 * AE-08 §69: Memory Provider.
 *
 * <p>Provides the store, retriever and
 * capabilities as a cohesive unit.
 * Memory backends (InMemory, JDBC, Redis,
 * Vector Store) implement this interface.
 */
public interface MemoryProvider {

    /**
     * Provider type identifier
     * (e.g. "inmemory", "jdbc", "redis").
     */
    String providerType();

    /**
     * Get the memory store.
     */
    MemoryStore store();

    /**
     * Get the memory retriever.
     */
    MemoryRetriever retriever();

    /**
     * Get provider capabilities.
     */
    MemoryCapabilities capabilities();

    /**
     * Memory provider capabilities.
     */
    record MemoryCapabilities(
        boolean supportsSemanticRetrieval,
        boolean supportsVersioning,
        boolean supportsExpiration,
        Collection<MemoryType> supportedTypes,
        Map<String, Object> metadata
    ) {

        public MemoryCapabilities {
            supportedTypes =
                supportedTypes == null
                    ? java.util.Set.copyOf(
                        java.util.Set.of(
                            MemoryType.values()
                        )
                    )
                    : java.util.Set.copyOf(
                        supportedTypes
                    );
            metadata =
                metadata == null
                    ? Map.of()
                    : Map.copyOf(metadata);
        }

        public static MemoryCapabilities basic() {
            return new MemoryCapabilities(
                false, false, true,
                null, Map.of()
            );
        }

        public static MemoryCapabilities full() {
            return new MemoryCapabilities(
                true, true, true,
                null, Map.of()
            );
        }
    }
}
