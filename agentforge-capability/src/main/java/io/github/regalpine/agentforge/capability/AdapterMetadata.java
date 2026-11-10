package io.github.regalpine.agentforge.capability;

import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Adapter metadata descriptor.
 * AF-ENHANCE-02 §3: describes a
 * CapabilityAdapter's identity, protocol
 * type, supported operations and constraints.
 *
 * <p>Each adapter produces exactly one
 * AdapterMetadata. The registry uses this
 * for discovery and routing.
 */
public record AdapterMetadata(
    String adapterId,
    String protocol,
    String version,
    String displayName,
    String description,
    Set<SideEffectClassification> supportedSideEffects,
    Map<String, Object> configurationSchema,
    Map<String, Object> metadata
) {

    public AdapterMetadata {
        Objects.requireNonNull(adapterId, "adapterId");
        Objects.requireNonNull(protocol, "protocol");
        Objects.requireNonNull(version, "version");

        if (adapterId.isBlank()) {
            throw new IllegalArgumentException(
                "adapterId cannot be blank"
            );
        }

        supportedSideEffects =
            supportedSideEffects == null
                ? Set.of(SideEffectClassification.READ_ONLY)
                : Set.copyOf(supportedSideEffects);

        configurationSchema =
            configurationSchema == null
                ? Map.of()
                : Map.copyOf(configurationSchema);

        metadata =
            metadata == null
                ? Map.of()
                : Map.copyOf(metadata);
    }
}
