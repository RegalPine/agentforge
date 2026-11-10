package io.github.regalpine.agentforge.kernel;

import java.util.Objects;

public record Reference(
    EntityId id,
    Version version
) {

    public Reference {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(version, "version");
    }

    public static Reference of(
        String id,
        String version
    ) {
        return new Reference(
            new EntityId(id),
            new Version(version)
        );
    }
}
