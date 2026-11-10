package io.github.regalpine.agentforge.rag.model;

import java.util.Objects;
import java.util.Set;

/**
 * Knowledge retrieval scope.
 * AE-09 §32: Knowledge Scope.
 *
 * <p>Defines the boundaries for a retrieval query:
 * which tenant, which sources, which documents, which classifications.
 */
public record KnowledgeScope(
    String tenantId,
    Set<String> sourceIds,
    Set<String> documentIds,
    Set<DocumentClassification> classifications
) {

    public KnowledgeScope {
        Objects.requireNonNull(tenantId, "tenantId");
        sourceIds = sourceIds == null ? Set.of() : Set.copyOf(sourceIds);
        documentIds = documentIds == null ? Set.of() : Set.copyOf(documentIds);
        classifications = classifications == null ? Set.of() : Set.copyOf(classifications);
    }

    public static KnowledgeScope tenant(String tenantId) {
        return new KnowledgeScope(tenantId, Set.of(), Set.of(), Set.of());
    }

    public static KnowledgeScope of(String tenantId, Set<String> sourceIds) {
        return new KnowledgeScope(tenantId, sourceIds, Set.of(), Set.of());
    }

    public boolean restrictsSources() {
        return !sourceIds.isEmpty();
    }

    public boolean restrictsDocuments() {
        return !documentIds.isEmpty();
    }

    public boolean restrictsClassifications() {
        return !classifications.isEmpty();
    }
}
