package io.github.regalpine.agentforge.rag.provider;

import java.util.Map;
import java.util.Set;

/**
 * Knowledge provider capabilities.
 * AE-09 §70: KnowledgeCapabilities.
 */
public record KnowledgeCapabilities(
    boolean supportsKeywordSearch,
    boolean supportsVectorSearch,
    boolean supportsHybridSearch,
    boolean supportsReranking,
    boolean supportsCitation,
    boolean supportsIncrementalIngestion,
    Set<String> supportedMimeTypes
) {

    public static KnowledgeCapabilities basic() {
        return new KnowledgeCapabilities(true, false, false, false, true, false,
                Set.of("text/plain", "text/markdown", "text/html"));
    }

    public static KnowledgeCapabilities full() {
        return new KnowledgeCapabilities(true, true, true, true, true, true,
                Set.of("text/plain", "text/markdown", "text/html", "application/pdf"));
    }
}
