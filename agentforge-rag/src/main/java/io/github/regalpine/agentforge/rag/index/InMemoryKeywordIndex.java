package io.github.regalpine.agentforge.rag.index;

import io.github.regalpine.agentforge.rag.model.*;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * In-memory keyword index.
 * AE-09 §28: Keyword Index.
 *
 * <p>Simple keyword-based index using TF term matching.
 * Suitable for exact term matching (product IDs, policy numbers, etc.).
 */
public final class InMemoryKeywordIndex implements KnowledgeIndex {

    private final Map<String, KnowledgeChunk> chunks = new ConcurrentHashMap<>();

    @Override
    public void upsert(KnowledgeChunk chunk) {
        Objects.requireNonNull(chunk, "chunk");
        chunks.put(chunk.chunkId(), chunk);
    }

    @Override
    public void delete(String chunkId) {
        chunks.remove(chunkId);
    }

    @Override
    public List<RetrievalResult> search(KnowledgeQuery query) {
        Objects.requireNonNull(query, "query");

        String normalizedQuery = query.query().toLowerCase();
        String[] queryTerms = normalizedQuery.split("\\s+");

        KnowledgeScope scope = query.scope();
        int topK = query.topK();

        List<RetrievalResult> results = new ArrayList<>();

        for (KnowledgeChunk chunk : chunks.values()) {
            if (!matchesScope(chunk, scope)) continue;

            String text = chunk.text().toLowerCase();
            double score = computeKeywordScore(text, queryTerms);

            if (score > 0.0) {
                results.add(new RetrievalResult(
                        chunk.chunkId(),
                        chunk.documentId(),
                        chunk.text(),
                        score,
                        RetrievalScoreBreakdown.keyword(score),
                        chunk.location(),
                        RetrievalProvenance.of(chunk, "")
                ));
            }
        }

        results.sort(Comparator.comparingDouble(RetrievalResult::score).reversed());
        return results.subList(0, Math.min(topK, results.size()));
    }

    @Override
    public String indexType() {
        return "KEYWORD";
    }

    @Override
    public int size() {
        return chunks.size();
    }

    private boolean matchesScope(KnowledgeChunk chunk, KnowledgeScope scope) {
        if (!chunk.tenantId().equals(scope.tenantId())) return false;
        if (scope.restrictsSources() && !scope.sourceIds().contains(chunk.sourceId())) return false;
        if (scope.restrictsDocuments() && !scope.documentIds().contains(chunk.documentId())) return false;
        if (scope.restrictsClassifications() && !scope.classifications().contains(chunk.classification())) return false;
        return true;
    }

    private double computeKeywordScore(String text, String[] terms) {
        double score = 0.0;
        for (String term : terms) {
            if (text.contains(term)) {
                int count = countOccurrences(text, term);
                score += Math.min(1.0, count * 0.2);
            }
        }
        return terms.length == 0 ? 0.0 : score / terms.length;
    }

    private int countOccurrences(String text, String term) {
        int count = 0;
        int idx = 0;
        while ((idx = text.indexOf(term, idx)) != -1) {
            count++;
            idx += term.length();
        }
        return count;
    }
}
