package io.github.regalpine.agentforge.rag.index;

import io.github.regalpine.agentforge.rag.model.*;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Hybrid index combining keyword and vector search.
 * AE-09 §30: Hybrid Search.
 *
 * <p>Merges results from keyword and vector indices,
 * deduplicates by chunk ID, and combines scores.
 */
public final class HybridIndex implements KnowledgeIndex {

    private final InMemoryKeywordIndex keywordIndex;
    private final InMemoryVectorIndex vectorIndex;
    private final double keywordWeight;
    private final double vectorWeight;

    public HybridIndex(double keywordWeight, double vectorWeight) {
        this.keywordIndex = new InMemoryKeywordIndex();
        this.vectorIndex = new InMemoryVectorIndex();
        this.keywordWeight = keywordWeight;
        this.vectorWeight = vectorWeight;
    }

    public HybridIndex() {
        this(0.3, 0.7);
    }

    public InMemoryKeywordIndex keywordIndex() {
        return keywordIndex;
    }

    public InMemoryVectorIndex vectorIndex() {
        return vectorIndex;
    }

    @Override
    public void upsert(KnowledgeChunk chunk) {
        keywordIndex.upsert(chunk);
        vectorIndex.upsert(chunk);
    }

    @Override
    public void delete(String chunkId) {
        keywordIndex.delete(chunkId);
        vectorIndex.delete(chunkId);
    }

    @Override
    public List<RetrievalResult> search(KnowledgeQuery query) {
        List<RetrievalResult> keywordResults = keywordIndex.search(query);
        return mergeAndRank(keywordResults, query);
    }

    /**
     * Hybrid search with a query embedding.
     */
    public List<RetrievalResult> search(KnowledgeQuery query, float[] queryEmbedding) {
        List<RetrievalResult> keywordResults = keywordIndex.search(query);
        List<RetrievalResult> vectorResults = vectorIndex.search(queryEmbedding, query.scope(), query.topK() * 2);
        return mergeAndRank(keywordResults, vectorResults, query.topK());
    }

    private List<RetrievalResult> mergeAndRank(List<RetrievalResult> keywordResults, KnowledgeQuery query) {
        int topK = query.topK();
        List<RetrievalResult> merged = new ArrayList<>(keywordResults);
        merged.sort(Comparator.comparingDouble(RetrievalResult::score).reversed());
        return merged.subList(0, Math.min(topK, merged.size()));
    }

    private List<RetrievalResult> mergeAndRank(
            List<RetrievalResult> keywordResults,
            List<RetrievalResult> vectorResults,
            int topK) {

        Map<String, RetrievalResult> merged = new LinkedHashMap<>();

        for (RetrievalResult r : keywordResults) {
            merged.put(r.chunkId(), r);
        }

        for (RetrievalResult r : vectorResults) {
            RetrievalResult existing = merged.get(r.chunkId());
            if (existing != null) {
                double combinedScore = keywordWeight * existing.score() + vectorWeight * r.score();
                RetrievalScoreBreakdown combined = RetrievalScoreBreakdown.of(
                        existing.scores().keywordScore() != null ? existing.scores().keywordScore() : 0.0,
                        r.scores().semanticScore() != null ? r.scores().semanticScore() : 0.0
                );
                merged.put(r.chunkId(), new RetrievalResult(
                        r.chunkId(), r.documentId(), r.text(),
                        combinedScore, combined, r.location(), r.provenance()
                ));
            } else {
                merged.put(r.chunkId(), r);
            }
        }

        return merged.values().stream()
                .sorted(Comparator.comparingDouble(RetrievalResult::score).reversed())
                .limit(topK)
                .collect(Collectors.toList());
    }

    @Override
    public String indexType() {
        return "HYBRID";
    }

    @Override
    public int size() {
        return keywordIndex.size();
    }
}
