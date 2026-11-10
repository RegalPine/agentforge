package io.github.regalpine.agentforge.rag.index;

import io.github.regalpine.agentforge.rag.model.*;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory vector index using cosine similarity.
 * AE-09 §29: Vector Index.
 *
 * <p>Stores chunk embeddings and performs brute-force cosine similarity search.
 * Suitable for semantic retrieval. For production, replace with a real vector DB.
 */
public final class InMemoryVectorIndex implements KnowledgeIndex {

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
        throw new UnsupportedOperationException(
                "Vector search requires an embedding model. Use HybridIndex or provide query embedding.");
    }

    /**
     * Search with a pre-computed query embedding.
     *
     * @param queryEmbedding the query vector
     * @param scope          the retrieval scope
     * @param topK           maximum results
     * @return ranked retrieval results
     */
    public List<RetrievalResult> search(float[] queryEmbedding, KnowledgeScope scope, int topK) {
        Objects.requireNonNull(queryEmbedding, "queryEmbedding");

        List<RetrievalResult> results = new ArrayList<>();

        for (KnowledgeChunk chunk : chunks.values()) {
            if (!matchesScope(chunk, scope)) continue;
            if (!chunk.hasEmbedding()) continue;

            double similarity = cosineSimilarity(queryEmbedding, chunk.embedding());
            if (similarity > 0.0) {
                results.add(new RetrievalResult(
                        chunk.chunkId(),
                        chunk.documentId(),
                        chunk.text(),
                        similarity,
                        RetrievalScoreBreakdown.semantic(similarity),
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
        return "VECTOR";
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

    public static double cosineSimilarity(float[] a, float[] b) {
        if (a.length != b.length) return 0.0;
        double dot = 0.0, normA = 0.0, normB = 0.0;
        for (int i = 0; i < a.length; i++) {
            dot += a[i] * b[i];
            normA += a[i] * a[i];
            normB += b[i] * b[i];
        }
        double denom = Math.sqrt(normA) * Math.sqrt(normB);
        return denom == 0.0 ? 0.0 : dot / denom;
    }
}
