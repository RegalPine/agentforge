package io.github.regalpine.agentforge.rag.model;

import java.util.Objects;

/**
 * Knowledge citation — traces an answer back to its source.
 * AE-09 §39: Citation.
 *
 * <p>A citation represents the provenance of a piece of information
 * in the model's output, linking it back to the original document chunk.
 */
public record KnowledgeCitation(
    String citationId,
    String documentId,
    String title,
    String sourceId,
    ChunkLocation location,
    String sourceReference
) {

    public KnowledgeCitation {
        Objects.requireNonNull(citationId, "citationId");
        Objects.requireNonNull(documentId, "documentId");
        title = title == null ? "" : title;
        sourceId = sourceId == null ? "" : sourceId;
        Objects.requireNonNull(location, "location");
        sourceReference = sourceReference == null ? "" : sourceReference;
    }

    public static KnowledgeCitation from(KnowledgeChunk chunk, String title) {
        return new KnowledgeCitation(
            "cite-" + chunk.chunkId(),
            chunk.documentId(),
            title,
            chunk.sourceId(),
            chunk.location(),
            chunk.sourceId() + "/" + chunk.documentId() + "#" + chunk.chunkId()
        );
    }
}
