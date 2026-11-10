package io.github.regalpine.agentforge.rag.model;

import java.util.Objects;

/**
 * Retrieval provenance — tracks the origin of a retrieved chunk.
 * AE-09 §38: Retrieval Provenance.
 */
public record RetrievalProvenance(
    String sourceId,
    String documentId,
    String documentVersion,
    String chunkId,
    ChunkLocation location,
    String checksum
) {

    public RetrievalProvenance {
        Objects.requireNonNull(sourceId, "sourceId");
        Objects.requireNonNull(documentId, "documentId");
        documentVersion = documentVersion == null ? "" : documentVersion;
        Objects.requireNonNull(chunkId, "chunkId");
        Objects.requireNonNull(location, "location");
        checksum = checksum == null ? "" : checksum;
    }

    public static RetrievalProvenance of(KnowledgeChunk chunk, String documentVersion) {
        return new RetrievalProvenance(
            chunk.sourceId(),
            chunk.documentId(),
            documentVersion,
            chunk.chunkId(),
            chunk.location(),
            chunk.checksum()
        );
    }
}
