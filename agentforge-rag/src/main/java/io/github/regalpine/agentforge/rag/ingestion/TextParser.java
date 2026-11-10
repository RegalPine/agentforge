package io.github.regalpine.agentforge.rag.ingestion;

import java.util.List;
import java.util.Map;

/**
 * Plain text parser.
 * AE-09 §14: Document Parser.
 *
 * <p>Treats the entire content as a single paragraph.
 * No structural elements are extracted.
 */
public final class TextParser implements DocumentParser {

    @Override
    public ParsedDocument parse(String documentId, String mimeType, String content) {
        String title = extractTitle(content);
        List<DocumentElement> elements = List.of(DocumentElement.paragraph(content));
        return new ParsedDocument(documentId, title, content, elements, Map.of());
    }

    @Override
    public boolean supports(String mimeType) {
        return "text/plain".equalsIgnoreCase(mimeType);
    }

    private String extractTitle(String content) {
        int newline = content.indexOf('\n');
        if (newline > 0 && newline < 200) {
            return content.substring(0, newline).trim();
        }
        return content.length() > 100 ? content.substring(0, 100) : content;
    }
}
