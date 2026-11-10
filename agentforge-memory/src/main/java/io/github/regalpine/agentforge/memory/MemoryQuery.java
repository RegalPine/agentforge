package io.github.regalpine.agentforge.memory;

import java.util.Objects;

/**
 * Memory query.
 * AE-08 §24: Memory Query.
 *
 * <p>Used to express retrieval intent.
 * The query is passed through policy,
 * scope filtering and the chosen
 * retrieval strategy.
 */
public record MemoryQuery(
    String subjectId,
    MemoryScope scope,
    MemoryType type,
    String query,
    int limit,
    RetrievalStrategy strategy
) {

    public MemoryQuery {
        Objects.requireNonNull(
            subjectId, "subjectId"
        );
        Objects.requireNonNull(
            scope, "scope"
        );

        query = query == null ? "" : query;

        if (limit <= 0) {
            limit = 10;
        }

        strategy =
            strategy == null
                ? RetrievalStrategy.EXACT
                : strategy;
    }

    public MemoryQuery(
        String subjectId,
        MemoryScope scope,
        MemoryType type,
        String query,
        int limit
    ) {
        this(
            subjectId, scope, type,
            query, limit,
            RetrievalStrategy.KEYWORD
        );
    }
}
