package io.github.regalpine.agentforge.rag.model;

import java.util.Objects;

/**
 * A single retrieval result.
 * AE-09 §36: Retrieval Result.
 */
public record RetrievalResult(
    String chunkId,
    String documentId,
    String text,
    double score,
    RetrievalScoreBreakdown scores,
    ChunkLocation location,
    RetrievalProvenance provenance
) {

    public RetrievalResult {
        Objects.requireNonNull(chunkId, "chunkId");
        Objects.requireNonNull(documentId, "documentId");
        Objects.requireNonNull(text, "text");
        Objects.requireNonNull(scores, "scores");
        Objects.requireNonNull(location, "location");
        Objects.requireNonNull(provenance, "provenance");
    }

    public static RetrievalResult of(KnowledgeChunk chunk, double score, RetrievalScoreBreakdown scores) {
        return new RetrievalResult(
            chunk.chunkId(),
            chunk.documentId(),
            chunk.text(),
            score,
            scores,
            chunk.location(),
            RetrievalProvenance.of(chunk, "")
        );
    }
}
