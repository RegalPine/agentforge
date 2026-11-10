package io.github.regalpine.agentforge.rag.citation;

import io.github.regalpine.agentforge.rag.model.KnowledgeCitation;
import io.github.regalpine.agentforge.rag.model.KnowledgeChunk;
import io.github.regalpine.agentforge.rag.model.RetrievalResult;

import java.util.*;

/**
 * Citation validator — validates that citations are grounded in retrieved knowledge.
 * AE-09 §62-64: Citation Validation, Grounded Answer.
 */
public final class CitationValidator {

    /**
     * Validate that a citation refers to a known retrieved chunk.
     *
     * @param citation the citation to validate
     * @param retrieved the set of retrieved results
     * @return true if the citation is valid (exists in retrieved knowledge)
     */
    public boolean validate(KnowledgeCitation citation, List<RetrievalResult> retrieved) {
        Objects.requireNonNull(citation, "citation");
        Objects.requireNonNull(retrieved, "retrieved");

        return retrieved.stream()
                .anyMatch(r -> r.documentId().equals(citation.documentId()));
    }

    /**
     * Generate citations from retrieval results.
     */
    public List<KnowledgeCitation> generateCitations(List<RetrievalResult> results, Map<String, String> documentTitles) {
        List<KnowledgeCitation> citations = new ArrayList<>();
        for (RetrievalResult result : results) {
            String title = documentTitles.getOrDefault(result.documentId(), result.documentId());
            KnowledgeChunk syntheticChunk = new KnowledgeChunk(
                    result.chunkId(), result.documentId(), "", "",
                    result.text(), result.location(), "", null, "en", null, null
            );
            citations.add(KnowledgeCitation.from(syntheticChunk, title));
        }
        return citations;
    }

    /**
     * Check if all citations in a list are grounded in the retrieved knowledge.
     */
    public GroundingResult checkGrounding(List<KnowledgeCitation> citations, List<RetrievalResult> retrieved) {
        int valid = 0;
        int invalid = 0;
        List<String> invalidCitationIds = new ArrayList<>();

        for (KnowledgeCitation citation : citations) {
            if (validate(citation, retrieved)) {
                valid++;
            } else {
                invalid++;
                invalidCitationIds.add(citation.citationId());
            }
        }

        return new GroundingResult(valid, invalid, invalidCitationIds);
    }

    /**
     * Result of grounding validation.
     */
    public record GroundingResult(
        int validCount,
        int invalidCount,
        List<String> invalidCitationIds
    ) {
        public boolean isFullyGrounded() {
            return invalidCount == 0;
        }

        public double groundingRatio() {
            int total = validCount + invalidCount;
            return total == 0 ? 1.0 : (double) validCount / total;
        }
    }
}
