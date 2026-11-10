package io.github.regalpine.agentforge.rag.ingestion;

/**
 * Document parser SPI.
 * AE-09 §14: Document Parser.
 *
 * <p>Parses raw document content into a structured ParsedDocument,
 * preserving headings, paragraphs, tables, etc.
 */
public interface DocumentParser {

    /**
     * Parse the given raw content into a structured document.
     *
     * @param documentId the document identifier
     * @param mimeType   the MIME type of the content
     * @param content    the raw content
     * @return the parsed document with structural elements
     */
    ParsedDocument parse(String documentId, String mimeType, String content);

    /**
     * Check if this parser supports the given MIME type.
     */
    boolean supports(String mimeType);
}
