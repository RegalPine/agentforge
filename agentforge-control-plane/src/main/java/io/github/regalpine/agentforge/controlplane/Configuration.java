package io.github.regalpine.agentforge.controlplane;

import java.time.Instant;
import java.util.Map;

/**
 * Versioned configuration.
 * AF-ENHANCE-14 §18: Configuration.
 *
 * <p>Configuration is versionable,
 * auditable, validatable, rollbackable.
 * Secrets must NOT appear here.
 */
public record Configuration(
    String configId,
    String version,
    Map<String, String> entries,
    String digest,
    Instant createdAt
) {

    public Configuration {
        if (configId == null
            || configId.isBlank()) {
            throw new IllegalArgumentException(
                "configId required");
        }
        if (version == null
            || version.isBlank()) {
            throw new IllegalArgumentException(
                "version required");
        }
        entries = entries == null
            ? Map.of() : Map.copyOf(entries);
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }

    /**
     * Create simple config.
     */
    public static Configuration of(
        String configId,
        String version,
        Map<String, String> entries
    ) {
        return new Configuration(
            configId, version,
            entries, null,
            Instant.now()
        );
    }
}
