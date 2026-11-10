package io.github.regalpine.agentforge.memory.redis;

import java.time.Duration;
import java.util.Map;
import java.util.Objects;

/**
 * Redis memory store configuration.
 * AF-ENHANCE-03 §5: configures the
 * Redis connection for session context,
 * transient and high-frequency memory
 * retrieval.
 */
public record RedisMemoryConfig(
    String host,
    int port,
    String keyPrefix,
    Duration defaultTtl,
    int maxMemoryBytes,
    String evictionPolicy,
    Map<String, Object> properties
) {

    public RedisMemoryConfig {
        Objects.requireNonNull(host, "host");
        Objects.requireNonNull(keyPrefix, "keyPrefix");

        if (port <= 0 || port > 65535) {
            port = 6379;
        }

        defaultTtl =
            defaultTtl == null
                ? Duration.ofHours(24)
                : defaultTtl;

        if (maxMemoryBytes <= 0) {
            maxMemoryBytes = 0;
        }

        if (evictionPolicy == null
            || evictionPolicy.isBlank()
        ) {
            evictionPolicy = "allkeys-lru";
        }

        properties =
            properties == null
                ? Map.of()
                : Map.copyOf(properties);
    }

    public static RedisMemoryConfig of(String host) {
        return new RedisMemoryConfig(
            host, 6379, "af:mem:",
            Duration.ofHours(24), 0,
            "allkeys-lru", Map.of()
        );
    }

    public static RedisMemoryConfig session(
        String host, Duration sessionTtl
    ) {
        return new RedisMemoryConfig(
            host, 6379, "af:session:",
            sessionTtl, 0,
            "allkeys-lru", Map.of()
        );
    }

    /**
     * Build the Redis key for a memory ID.
     */
    public String memoryKey(String memoryId) {
        return keyPrefix + memoryId;
    }

    /**
     * Build the Redis key for a subject index.
     */
    public String subjectIndexKey(String subjectId) {
        return keyPrefix + "idx:subject:" + subjectId;
    }

    /**
     * Build the Redis key for a type index.
     */
    public String typeIndexKey(
        String subjectId, String type
    ) {
        return keyPrefix + "idx:type:"
            + subjectId + ":" + type;
    }
}
