package io.github.regalpine.agentforge.provider.spi.registry;

import io.github.regalpine.agentforge.capability.CapabilityProvider;
import io.github.regalpine.agentforge.capability.ProviderDescriptor;
import io.github.regalpine.agentforge.kernel.Reference;
import io.github.regalpine.agentforge.kernel.Version;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.ServiceLoader;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class DefaultProviderRegistry
    implements ProviderRegistry {

    private final Map<
        String,
        CapabilityProvider
    > providers = new ConcurrentHashMap<>();

    @Override
    public void register(
        CapabilityProvider provider
    ) {
        ProviderDescriptor desc =
            provider.descriptor();

        String key = compositeKey(
            desc.providerId(),
            desc.version()
        );

        CapabilityProvider existing =
            providers.putIfAbsent(key, provider);

        if (existing != null) {
            throw new IllegalStateException(
                "Provider already registered: "
                    + desc.providerId()
                    + "@"
                    + desc.version()
            );
        }
    }

    @Override
    public Optional<CapabilityProvider> find(
        String providerId,
        Version version
    ) {
        return Optional.ofNullable(
            providers.get(
                compositeKey(providerId, version)
            )
        );
    }

    @Override
    public List<CapabilityProvider> findByCapability(
        Reference capability
    ) {
        List<CapabilityProvider> result =
            new ArrayList<>();

        for (CapabilityProvider provider :
            providers.values()
        ) {
            ProviderDescriptor desc =
                provider.descriptor();

            if (desc.capabilities()
                .contains(capability)
            ) {
                result.add(provider);
            }
        }

        return List.copyOf(result);
    }

    @Override
    public void unregister(
        String providerId,
        Version version
    ) {
        providers.remove(
            compositeKey(providerId, version)
        );
    }

    @Override
    public List<CapabilityProvider> all() {
        return List.copyOf(
            providers.values()
        );
    }

    @Override
    public int autoDiscover() {
        Logger logger =
            Logger.getLogger(
                DefaultProviderRegistry.class.getName()
            );

        int count = 0;

        ServiceLoader<CapabilityProvider> loader =
            ServiceLoader.load(CapabilityProvider.class);

        for (CapabilityProvider provider : loader) {
            try {
                register(provider);
                count++;

                logger.info(
                    "Auto-discovered provider: "
                        + provider.descriptor().providerId()
                        + "@"
                        + provider.descriptor().version()
                );
            } catch (Exception e) {
                logger.log(
                    Level.WARNING,
                    "Failed to auto-discover provider: "
                        + e.getMessage(),
                    e
                );
            }
        }

        logger.info(
            "Auto-discovery complete: "
                + count
                + " provider(s) registered"
        );

        return count;
    }

    private static String compositeKey(
        String providerId,
        Version version
    ) {
        return providerId + "@" + version.value();
    }
}
