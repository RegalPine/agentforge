package io.github.regalpine.agentforge.capability;

import io.github.regalpine.agentforge.kernel.Reference;
import io.github.regalpine.agentforge.kernel.Version;

import java.util.Map;
import java.util.Set;

public record ProviderDescriptor(
    String providerId,
    Version version,
    Set<Reference> capabilities,
    Map<String, Object> metadata
) {

    public ProviderDescriptor {
        if (providerId == null || providerId.isBlank()) {
            throw new IllegalArgumentException(
                "providerId cannot be blank"
            );
        }

        capabilities =
            capabilities == null
                ? Set.of()
                : Set.copyOf(capabilities);

        metadata =
            metadata == null
                ? Map.of()
                : Map.copyOf(metadata);
    }
}
