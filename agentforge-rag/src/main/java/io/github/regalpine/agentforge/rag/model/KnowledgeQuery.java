package io.github.regalpine.agentforge.rag.model;

import java.util.Map;
import java.util.Objects;

/**
 * Knowledge retrieval query.
 * AE-09 §31: Retrieval Query.
 */
public record KnowledgeQuery(
    String query,
    KnowledgeScope scope,
    Map<String, Object> filters,
    int topK
) {

    public KnowledgeQuery {
        Objects.requireNonNull(query, "query");
        Objects.requireNonNull(scope, "scope");
        filters = filters == null ? Map.of() : Map.copyOf(filters);
        if (topK <= 0) {
            topK = 10;
        }
    }

    public static KnowledgeQuery of(String query, String tenantId) {
        return new KnowledgeQuery(query, KnowledgeScope.tenant(tenantId), Map.of(), 10);
    }

    public static KnowledgeQuery of(String query, KnowledgeScope scope, int topK) {
        return new KnowledgeQuery(query, scope, Map.of(), topK);
    }
}
