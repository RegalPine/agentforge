package io.github.regalpine.agentforge.kernel;

import java.util.Objects;

/**
 * Typed reference to a Capability entity.
 * AF-10 §11: CapabilityRef.
 *
 * <p>Provides type-safe capability references
 * distinct from generic {@link Reference}.
 */
public record CapabilityRef(
    EntityId id,
    Version version
) {

    public CapabilityRef {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(version, "version");
    }

    public static CapabilityRef of(
        String id,
        String version
    ) {
        return new CapabilityRef(
            new EntityId(id),
            new Version(version)
        );
    }

    public Reference toReference() {
        return new Reference(id, version);
    }
}
