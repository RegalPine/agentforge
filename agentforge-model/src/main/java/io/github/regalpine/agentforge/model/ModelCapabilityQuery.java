package io.github.regalpine.agentforge.model;

import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Model capability query —
 * AF-ENHANCE-09 §21, §23.
 *
 * <p>Query for finding models
 * in the registry by capabilities,
 * provider, version.
 */
public record ModelCapabilityQuery(
    Set<String> requiredCapabilities,
    String providerId,
    String versionConstraint,
    boolean includeDisabled,
    Map<String, String> metadataFilter
) {

    public ModelCapabilityQuery {
        requiredCapabilities =
            requiredCapabilities == null
                ? Set.of()
                : Set.copyOf(requiredCapabilities);
        metadataFilter =
            metadataFilter == null
                ? Map.of()
                : Map.copyOf(metadataFilter);
    }

    /**
     * Query by capabilities.
     */
    public static ModelCapabilityQuery of(
        Set<String> capabilities
    ) {
        return new ModelCapabilityQuery(
            capabilities, null, null,
            false, null
        );
    }

    /**
     * Query by provider.
     */
    public static ModelCapabilityQuery byProvider(
        String providerId
    ) {
        return new ModelCapabilityQuery(
            null, providerId, null,
            false, null
        );
    }

    /**
     * Match a definition against
     * this query.
     */
    public boolean matches(
        ModelDefinition def
    ) {
        // Check provider
        if (providerId != null
            && !providerId.isEmpty()
        ) {
            if (!def.providerId().equals(
                providerId
            )) {
                return false;
            }
        }

        // Check capabilities
        for (String cap : requiredCapabilities) {
            if (!def.supports(cap)) {
                return false;
            }
        }

        return true;
    }
}
