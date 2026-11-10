package io.github.regalpine.agentforge.memory.redis;

import io.github.regalpine.agentforge.memory.*;

import java.time.Instant;
import java.util.*;

/**
 * Redis memory retriever.
 * AF-ENHANCE-03 §5: supports EXACT and
 * KEYWORD search for high-frequency
 * session context retrieval.
 *
 * <p>Redis is optimised for EXACT and
 * keyword lookups via index sets.
 * SEMANTIC search is not natively
 * supported (use PostgreSQL + pgvector
 * for that).
 */
public final class RedisMemoryRetriever
    implements MemoryRetriever {

    private final RedisMemoryStore store;
    private final RedisMemoryStore.RedisClient client;
    private final RedisMemoryConfig config;

    public RedisMemoryRetriever(
        RedisMemoryStore store,
        RedisMemoryStore.RedisClient client,
        RedisMemoryConfig config
    ) {
        this.store =
            Objects.requireNonNull(store, "store");
        this.client =
            Objects.requireNonNull(client, "client");
        this.config =
            Objects.requireNonNull(config, "config");
    }

    @Override
    public List<MemoryRecord> retrieve(
        MemoryQuery query
    ) {
        Objects.requireNonNull(query, "query");

        return switch (query.strategy()) {
            case EXACT -> retrieveExact(query);
            case KEYWORD -> retrieveKeyword(query);
            case SEMANTIC, HYBRID ->
                throw new UnsupportedOperationException(
                    "Redis does not support "
                        + query.strategy()
                        + " search. Use PostgreSQL"
                        + " + pgvector instead."
                );
        };
    }

    private List<MemoryRecord> retrieveExact(
        MemoryQuery query
    ) {
        Set<String> ids;

        if (query.type() != null) {
            ids = store.findBySubjectAndType(
                query.subjectId(), query.type()
            );
        } else {
            ids = store.findBySubject(
                query.subjectId()
            );
        }

        List<MemoryRecord> results = new ArrayList<>();
        Instant now = Instant.now();

        for (String id : ids) {
            store.get(id).ifPresent(r -> {
                if (r.isRetrievable(now)) {
                    results.add(r);
                }
            });

            if (results.size() >= query.limit()) {
                break;
            }
        }

        return results;
    }

    private List<MemoryRecord> retrieveKeyword(
        MemoryQuery query
    ) {
        Set<String> ids = store.findBySubject(
            query.subjectId()
        );

        String lowerQuery =
            query.query().toLowerCase();
        String[] terms = lowerQuery.split("\\s+");

        List<ScoredMemory> scored = new ArrayList<>();
        Instant now = Instant.now();

        for (String id : ids) {
            Optional<MemoryRecord> opt = store.get(id);

            if (opt.isEmpty()) continue;

            MemoryRecord r = opt.get();

            if (!r.isRetrievable(now)) continue;

            String lowerContent =
                r.content().toLowerCase();

            double score = 0.0;

            for (String term : terms) {
                if (lowerContent.contains(term)) {
                    score += 1.0;
                }
            }

            if (score > 0.0) {
                score /= terms.length;
                scored.add(new ScoredMemory(r, score));
            }
        }

        scored.sort(Comparator
            .comparingDouble(
                (ScoredMemory s) -> s.score
            ).reversed()
        );

        List<MemoryRecord> results = new ArrayList<>();

        for (int i = 0;
             i < Math.min(
                 query.limit(), scored.size()
             );
             i++
        ) {
            results.add(scored.get(i).record);
        }

        return results;
    }

    private record ScoredMemory(
        MemoryRecord record, double score
    ) {}
}
