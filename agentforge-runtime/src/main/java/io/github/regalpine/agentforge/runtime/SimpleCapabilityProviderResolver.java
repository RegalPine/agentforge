package io.github.regalpine.agentforge.runtime;

import io.github.regalpine.agentforge.capability.CapabilityProvider;
import io.github.regalpine.agentforge.kernel.Reference;

import java.util.Map;

public final class SimpleCapabilityProviderResolver
    implements CapabilityProviderResolver {

    private final Map<
        Reference,
        CapabilityProvider
    > providers;

    public SimpleCapabilityProviderResolver(
        Map<Reference, CapabilityProvider> providers
    ) {
        this.providers =
            Map.copyOf(providers);
    }

    @Override
    public CapabilityProvider resolve(
        Reference capability
    ) {
        CapabilityProvider provider =
            providers.get(capability);

        if (provider == null) {
            throw new IllegalStateException(
                "No provider for capability: "
                    + capability
            );
        }

        return provider;
    }
}
