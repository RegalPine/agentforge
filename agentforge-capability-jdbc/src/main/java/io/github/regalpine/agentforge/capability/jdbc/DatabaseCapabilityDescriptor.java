package io.github.regalpine.agentforge.capability.jdbc;

import io.github.regalpine.agentforge.capability.SideEffectClassification;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Database capability descriptor.
 * AF-ENHANCE-02 §12: describes a
 * database operation that can be
 * invoked as a capability.
 *
 * <p>Security constraint: LLM must
 * NEVER generate arbitrary SQL.
 * All queries must be pre-defined
 * parameterised statements.
 */
public record DatabaseCapabilityDescriptor(
    String capabilityId,
    String name,
    String description,
    String tableName,
    DatabaseOperation operation,
    String parameterisedSql,
    List<String> parameterNames,
    Set<String> allowedColumns,
    SideEffectClassification sideEffect,
    int maxRows,
    Map<String, Object> metadata
) {

    public DatabaseCapabilityDescriptor {
        Objects.requireNonNull(
            capabilityId, "capabilityId"
        );

        Objects.requireNonNull(
            name, "name"
        );

        Objects.requireNonNull(
            parameterisedSql, "parameterisedSql"
        );

        Objects.requireNonNull(
            operation, "operation"
        );

        if (capabilityId.isBlank()) {
            throw new IllegalArgumentException(
                "capabilityId cannot be blank"
            );
        }

        // Security: reject SQL containing
        // raw string concatenation patterns
        String upperSql =
            parameterisedSql.toUpperCase();

        if (upperSql.contains("'")
            && !parameterisedSql.contains("?")
        ) {
            throw new IllegalArgumentException(
                "SQL must use parameterised "
                    + "queries (? placeholders). "
                    + "Raw string literals are forbidden."
            );
        }

        parameterNames =
            parameterNames == null
                ? List.of()
                : List.copyOf(parameterNames);

        allowedColumns =
            allowedColumns == null
                ? Set.of()
                : Set.copyOf(allowedColumns);

        sideEffect =
            sideEffect == null
                ? SideEffectClassification.READ_ONLY
                : sideEffect;

        if (maxRows <= 0) {
            maxRows = 1000;
        }

        metadata =
            metadata == null
                ? Map.of()
                : Map.copyOf(metadata);
    }
}
