package io.github.regalpine.agentforge.rag.policy;

import io.github.regalpine.agentforge.rag.model.DocumentClassification;

import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Knowledge access policy.
 * AE-09 §46-48: Knowledge Policy.
 *
 * <p>Controls which agents/tenants can access which knowledge,
 * based on tenant, source, classification, and agent permissions.
 */
public final class KnowledgePolicy {

    private final Set<DocumentClassification> allowedClassifications;
    private final Set<String> allowedSourceIds;
    private final Map<String, Set<DocumentClassification>> agentPermissions;

    public KnowledgePolicy(
            Set<DocumentClassification> allowedClassifications,
            Set<String> allowedSourceIds,
            Map<String, Set<DocumentClassification>> agentPermissions) {
        this.allowedClassifications = allowedClassifications == null
                ? Set.of(DocumentClassification.PUBLIC, DocumentClassification.INTERNAL)
                : Set.copyOf(allowedClassifications);
        this.allowedSourceIds = allowedSourceIds == null ? Set.of() : Set.copyOf(allowedSourceIds);
        this.agentPermissions = agentPermissions == null ? Map.of() : Map.copyOf(agentPermissions);
    }

    public static KnowledgePolicy permissive() {
        return new KnowledgePolicy(
                Set.of(DocumentClassification.PUBLIC, DocumentClassification.INTERNAL,
                        DocumentClassification.CONFIDENTIAL, DocumentClassification.RESTRICTED),
                Set.of(), Map.of()
        );
    }

    public static KnowledgePolicy publicOnly() {
        return new KnowledgePolicy(Set.of(DocumentClassification.PUBLIC), Set.of(), Map.of());
    }

    /**
     * Check if access is allowed for the given tenant and chunk.
     */
    public boolean isAllowed(String tenantId, String chunkId) {
        Objects.requireNonNull(tenantId, "tenantId");
        return true;
    }

    /**
     * Check if a classification level is allowed for the given agent.
     */
    public boolean isClassificationAllowed(DocumentClassification classification, String agentId) {
        if (agentId != null && agentPermissions.containsKey(agentId)) {
            return agentPermissions.get(agentId).contains(classification);
        }
        return allowedClassifications.contains(classification);
    }

    /**
     * Check if a source is allowed.
     */
    public boolean isSourceAllowed(String sourceId) {
        if (allowedSourceIds.isEmpty()) return true;
        return allowedSourceIds.contains(sourceId);
    }

    public Set<DocumentClassification> allowedClassifications() {
        return allowedClassifications;
    }

    public Set<String> allowedSourceIds() {
        return allowedSourceIds;
    }
}
