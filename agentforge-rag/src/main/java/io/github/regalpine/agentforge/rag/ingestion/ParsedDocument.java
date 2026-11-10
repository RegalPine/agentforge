package io.github.regalpine.agentforge.rag.ingestion;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Parsed document — the output of a DocumentParser.
 * AE-09 §15: Parsed Document.
 *
 * <p>Contains the full text, structural elements, and metadata
 * extracted during parsing. Structure is preserved for chunking.
 */
public record ParsedDocument(
    String documentId,
    String title,
    String text,
    List<DocumentElement> elements,
    Map<String, Object> metadata
) {

    public ParsedDocument {
        Objects.requireNonNull(documentId, "documentId");
        title = title == null ? "" : title;
        Objects.requireNonNull(text, "text");
        elements = elements == null ? List.of() : List.copyOf(elements);
        metadata = metadata == null ? Map.of() : Map.copyOf(metadata);
    }

    public static ParsedDocument plain(String documentId, String text) {
        return new ParsedDocument(documentId, "", text, List.of(), Map.of());
    }

    public int elementCount() {
        return elements.size();
    }

    public boolean hasStructure() {
        return !elements.isEmpty();
    }
}
