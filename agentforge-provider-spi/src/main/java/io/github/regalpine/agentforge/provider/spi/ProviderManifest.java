package io.github.regalpine.agentforge.provider.spi;

import io.github.regalpine.agentforge.kernel.Version;

import java.util.List;

public record ProviderManifest(
    String providerId,
    Version version,
    List<ProviderCapabilityEntry> capabilities
) {

    public ProviderManifest {
        capabilities =
            capabilities == null
                ? List.of()
                : List.copyOf(capabilities);
    }
}
