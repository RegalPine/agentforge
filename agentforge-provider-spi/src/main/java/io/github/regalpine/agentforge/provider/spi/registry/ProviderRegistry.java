package io.github.regalpine.agentforge.provider.spi.registry;

import io.github.regalpine.agentforge.capability.CapabilityProvider;
import io.github.regalpine.agentforge.kernel.Reference;
import io.github.regalpine.agentforge.kernel.Version;

import java.util.List;
import java.util.Optional;

public interface ProviderRegistry {

    void register(
        CapabilityProvider provider
    );

    Optional<CapabilityProvider> find(
        String providerId,
        Version version
    );

    List<CapabilityProvider> findByCapability(
        Reference capability
    );

    void unregister(
        String providerId,
        Version version
    );

    List<CapabilityProvider> all();

    /**
     * Auto-discover and register providers via {@link java.util.ServiceLoader}.
     *
     * Scans the classpath for {@code META-INF/services/io.github.regalpine.agentforge.capability.CapabilityProvider}
     * and registers all discovered implementations.
     *
     * Providers that fail to load or register are logged and skipped.
     *
     * @return the number of providers successfully auto-discovered and registered
     */
    int autoDiscover();
}
