package io.github.regalpine.agentforge.rag.ingestion;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * A structural element within a parsed document.
 * AE-09 §15: Parsed Document / Document Element.
 */
public record DocumentElement(
    DocumentElementType type,
    String content,
    int level,
    Map<String, Object> metadata
) {

    public DocumentElement {
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(content, "content");
        metadata = metadata == null ? Map.of() : Map.copyOf(metadata);
    }

    public static DocumentElement heading(String text, int level) {
        return new DocumentElement(DocumentElementType.HEADING, text, level, Map.of());
    }

    public static DocumentElement paragraph(String text) {
        return new DocumentElement(DocumentElementType.PARAGRAPH, text, 0, Map.of());
    }

    public static DocumentElement code(String text) {
        return new DocumentElement(DocumentElementType.CODE, text, 0, Map.of());
    }

    public static DocumentElement table(String text) {
        return new DocumentElement(DocumentElementType.TABLE, text, 0, Map.of());
    }

    public static DocumentElement list(String text) {
        return new DocumentElement(DocumentElementType.LIST, text, 0, Map.of());
    }
}
