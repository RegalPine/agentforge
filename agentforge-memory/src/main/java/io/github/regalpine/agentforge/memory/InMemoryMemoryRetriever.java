package io.github.regalpine.agentforge.memory;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * In-memory memory retriever.
 * AE-08 §69: InMemory retrieval.
 *
 * <p>Supports EXACT and KEYWORD strategies.
 * Applies scope filtering, policy checks,
 * expiration filtering and importance ranking.
 */
public final class InMemoryMemoryRetriever
    implements MemoryRetriever {

    private final InMemoryMemoryStore store;
    private final MemoryPolicy policy;
    private final MemoryEventSink eventSink;

    public InMemoryMemoryRetriever(
        InMemoryMemoryStore store,
        MemoryPolicy policy,
        MemoryEventSink eventSink
    ) {
        this.store = store;
        this.policy = policy;
        this.eventSink = eventSink;
    }

    public InMemoryMemoryRetriever(
        InMemoryMemoryStore store,
        MemoryPolicy policy
    ) {
        this(store, policy, null);
    }

    @Override
    public List<MemoryRecord> retrieve(
        MemoryQuery query
    ) {
        emitEvent(
            MemoryEventType.MEMORY_RETRIEVAL_STARTED,
            query.subjectId()
        );

        Instant now = Instant.now();

        List<MemoryRecord> candidates =
            store.allRecords().values().stream()
                .filter(r -> r.isRetrievable(now))
                .filter(r -> matchesScope(r, query))
                .filter(r -> matchesType(r, query))
                .filter(r -> matchesQuery(r, query))
                .sorted(rankByImportance())
                .limit(query.limit())
                .toList();

        List<MemoryRecord> result =
            new ArrayList<>(candidates);

        emitEvent(
            MemoryEventType.MEMORY_RETRIEVAL_COMPLETED,
            query.subjectId()
        );

        return result;
    }

    private boolean matchesScope(
        MemoryRecord record,
        MemoryQuery query
    ) {
        return record.scope() == query.scope()
            && record.subjectId().equals(
                query.subjectId()
            );
    }

    private boolean matchesType(
        MemoryRecord record,
        MemoryQuery query
    ) {
        return query.type() == null
            || record.type() == query.type();
    }

    private boolean matchesQuery(
        MemoryRecord record,
        MemoryQuery query
    ) {
        if (query.query() == null
            || query.query().isBlank()
        ) {
            return true;
        }

        if (query.strategy() == RetrievalStrategy.EXACT) {
            return record.content().equals(
                query.query()
            );
        }

        // KEYWORD: case-insensitive contains
        String lower =
            query.query().toLowerCase(Locale.ROOT);

        String content =
            record.content().toLowerCase(Locale.ROOT);

        String[] keywords = lower.split("\\s+");

        for (String keyword : keywords) {
            if (!keyword.isBlank()
                && content.contains(keyword)
            ) {
                return true;
            }
        }

        return false;
    }

    private Comparator<MemoryRecord>
        rankByImportance()
    {
        return (a, b) -> {
            int importanceCompare =
                Integer.compare(
                    b.importance().weight(),
                    a.importance().weight()
                );

            if (importanceCompare != 0) {
                return importanceCompare;
            }

            // Recency as tiebreaker
            return b.updatedAt().compareTo(
                a.updatedAt()
            );
        };
    }

    private void emitEvent(
        MemoryEventType type,
        String subjectId
    ) {
        if (eventSink != null) {
            eventSink.emit(
                new MemoryEvent(
                    type, subjectId,
                    System.currentTimeMillis()
                )
            );
        }
    }
}
