package io.github.regalpine.agentforge.rag.retrieval;

import io.github.regalpine.agentforge.rag.index.KnowledgeIndex;
import io.github.regalpine.agentforge.rag.model.*;
import io.github.regalpine.agentforge.rag.policy.KnowledgePolicy;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Retrieval pipeline — the full retrieval chain.
 * AE-09 §33: Retrieval Pipeline.
 *
 * <p>Query → Normalize → Scope Filter → Permission Check →
 * Keyword Search → Vector Search → Merge → Deduplicate →
 * Rerank → Policy Filter → TopK
 */
public final class RetrievalPipeline implements KnowledgeRetriever {

    private final KnowledgeIndex index;
    private final KnowledgePolicy policy;
    private final RetrievalBudget budget;

    public RetrievalPipeline(KnowledgeIndex index, KnowledgePolicy policy, RetrievalBudget budget) {
        this.index = Objects.requireNonNull(index, "index");
        this.policy = policy;
        this.budget = budget == null ? RetrievalBudget.defaults() : budget;
    }

    public RetrievalPipeline(KnowledgeIndex index) {
        this(index, null, RetrievalBudget.defaults());
    }

    @Override
    public List<RetrievalResult> retrieve(KnowledgeQuery query) {
        Objects.requireNonNull(query, "query");

        int effectiveTopK = budget.effectiveTopK(query.topK());
        KnowledgeQuery boundedQuery = new KnowledgeQuery(
                query.query(), query.scope(), query.filters(), effectiveTopK
        );

        List<RetrievalResult> results = index.search(boundedQuery);

        results = deduplicate(results);

        if (policy != null) {
            results = applyPolicy(results, query.scope().tenantId());
        }

        return results.subList(0, Math.min(effectiveTopK, results.size()));
    }

    private List<RetrievalResult> deduplicate(List<RetrievalResult> results) {
        Set<String> seen = new HashSet<>();
        List<RetrievalResult> deduped = new ArrayList<>();
        for (RetrievalResult r : results) {
            if (seen.add(r.chunkId())) {
                deduped.add(r);
            }
        }
        return deduped;
    }

    private List<RetrievalResult> applyPolicy(List<RetrievalResult> results, String tenantId) {
        return results.stream()
                .filter(r -> policy.isAllowed(tenantId, r.chunkId()))
                .collect(Collectors.toList());
    }
}
