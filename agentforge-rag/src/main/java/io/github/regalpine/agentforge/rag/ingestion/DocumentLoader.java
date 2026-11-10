package io.github.regalpine.agentforge.rag.ingestion;

import java.io.InputStream;

/**
 * Document loader SPI.
 * AF-ENHANCE-03 §12: loads documents
 * from various sources (files, URLs,
 * streams) and returns their raw content
 * with a detected MIME type.
 *
 * <p>Implementations handle different
 * input sources. The loader is the first
 * stage of the RAG ingestion pipeline:
 * Load → Parse → Chunk → Embed → Index.
 */
public interface DocumentLoader {

    /**
     * Load a document from the given source.
     *
     * @param source the source identifier
     *               (file path, URL, etc.)
     * @return the loaded document content
     */
    LoadedDocument load(String source);

    /**
     * Load a document from an input stream.
     *
     * @param source   the source identifier
     * @param stream   the input stream
     * @param mimeType the MIME type hint
     * @return the loaded document content
     */
    LoadedDocument load(
        String source,
        InputStream stream,
        String mimeType
    );

    /**
     * Check if this loader supports the
     * given source type.
     */
    boolean supports(String source);

    /**
     * A loaded document with its raw
     * content and detected MIME type.
     */
    record LoadedDocument(
        String documentId,
        String mimeType,
        String content,
        java.util.Map<String, Object> metadata
    ) {

        public LoadedDocument {
            java.util.Objects.requireNonNull(
                documentId, "documentId"
            );
            java.util.Objects.requireNonNull(
                mimeType, "mimeType"
            );
            java.util.Objects.requireNonNull(
                content, "content"
            );

            metadata = metadata == null
                ? java.util.Map.of()
                : java.util.Map.copyOf(metadata);
        }
    }
}
