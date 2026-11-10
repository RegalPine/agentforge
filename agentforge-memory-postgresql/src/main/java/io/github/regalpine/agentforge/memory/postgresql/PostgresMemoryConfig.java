package io.github.regalpine.agentforge.memory.postgresql;

import java.time.Duration;
import java.util.Map;
import java.util.Objects;

/**
 * PostgreSQL memory store configuration.
 * AF-ENHANCE-03 §5-7: configures the
 * JDBC connection, schema, pooling and
 * search capabilities.
 */
public record PostgresMemoryConfig(
    String jdbcUrl,
    String username,
    String schema,
    String tableName,
    int maxPoolSize,
    Duration connectionTimeout,
    boolean enablePgVector,
    int embeddingDimension,
    Map<String, Object> properties
) {

    public PostgresMemoryConfig {
        Objects.requireNonNull(jdbcUrl, "jdbcUrl");
        Objects.requireNonNull(schema, "schema");

        if (jdbcUrl.isBlank()) {
            throw new IllegalArgumentException(
                "jdbcUrl cannot be blank"
            );
        }

        if (maxPoolSize <= 0) {
            maxPoolSize = 10;
        }

        connectionTimeout =
            connectionTimeout == null
                ? Duration.ofSeconds(30)
                : connectionTimeout;

        if (embeddingDimension <= 0) {
            embeddingDimension = 1536;
        }

        properties =
            properties == null
                ? Map.of()
                : Map.copyOf(properties);
    }

    public static PostgresMemoryConfig of(
        String jdbcUrl, String schema
    ) {
        return new PostgresMemoryConfig(
            jdbcUrl, null, schema,
            "memory_records",
            10, Duration.ofSeconds(30),
            false, 1536, Map.of()
        );
    }

    public PostgresMemoryConfig withPgVector(
        int dimension
    ) {
        return new PostgresMemoryConfig(
            jdbcUrl, username, schema,
            tableName, maxPoolSize,
            connectionTimeout, true,
            dimension, properties
        );
    }

    public String qualifiedTable() {
        return schema + "." + tableName;
    }
}
