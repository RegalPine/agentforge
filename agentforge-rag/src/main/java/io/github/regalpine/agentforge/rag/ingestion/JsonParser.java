package io.github.regalpine.agentforge.rag.ingestion;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * JSON document parser.
 * AF-ENHANCE-03 §12: parses JSON content
 * into structured elements. Top-level
 * objects become paragraphs; arrays
 * become list elements.
 *
 * <p>Uses a lightweight approach without
 * external JSON libraries. Extracts
 * key-value pairs from JSON objects.
 */
public final class JsonParser
    implements DocumentParser {

    @Override
    public ParsedDocument parse(
        String documentId,
        String mimeType,
        String content
    ) {
        String trimmed = content.trim();

        if (trimmed.isEmpty()) {
            return ParsedDocument.plain(
                documentId, content
            );
        }

        List<DocumentElement> elements =
            new ArrayList<>();

        StringBuilder fullText =
            new StringBuilder();

        if (trimmed.startsWith("{")) {
            parseObject(trimmed, elements, fullText);
        } else if (trimmed.startsWith("[")) {
            parseArray(trimmed, elements, fullText);
        } else {
            elements.add(
                DocumentElement.paragraph(trimmed)
            );
            fullText.append(trimmed);
        }

        return new ParsedDocument(
            documentId, "",
            fullText.toString(),
            elements,
            Map.of("format", "json")
        );
    }

    @Override
    public boolean supports(String mimeType) {
        return "application/json".equals(mimeType);
    }

    private void parseObject(
        String json,
        List<DocumentElement> elements,
        StringBuilder fullText
    ) {
        // Extract key-value pairs using
        // simple regex-free parsing.
        String inner = json.substring(
            1, json.length() - 1
        ).trim();

        String[] pairs = splitTopLevel(inner);

        for (String pair : pairs) {
            int colonIdx = pair.indexOf(':');

            if (colonIdx < 0) continue;

            String key = pair.substring(
                0, colonIdx
            ).trim();

            String value = pair.substring(
                colonIdx + 1
            ).trim();

            // Strip quotes from key
            if (key.startsWith("\"")
                && key.endsWith("\"")
            ) {
                key = key.substring(
                    1, key.length() - 1
                );
            }

            // Strip quotes from value
            if (value.startsWith("\"")
                && value.endsWith("\"")
            ) {
                value = value.substring(
                    1, value.length() - 1
                );
            }

            String entry = key + ": " + value;

            elements.add(
                DocumentElement.paragraph(entry)
            );
            fullText.append(entry).append("\n");
        }
    }

    private void parseArray(
        String json,
        List<DocumentElement> elements,
        StringBuilder fullText
    ) {
        String inner = json.substring(
            1, json.length() - 1
        ).trim();

        String[] items = splitTopLevel(inner);

        StringBuilder listText =
            new StringBuilder();

        for (int i = 0; i < items.length; i++) {
            String item = items[i].trim();

            if (item.startsWith("\"")
                && item.endsWith("\"")
            ) {
                item = item.substring(
                    1, item.length() - 1
                );
            }

            listText.append("- ")
                    .append(item)
                    .append("\n");
        }

        elements.add(DocumentElement.list(
            listText.toString()
        ));
        fullText.append(listText);
    }

    private String[] splitTopLevel(String s) {
        List<String> parts = new ArrayList<>();
        int depth = 0;
        boolean inQuotes = false;
        StringBuilder current = new StringBuilder();

        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);

            if (c == '"'
                && (i == 0
                    || s.charAt(i - 1) != '\\')
            ) {
                inQuotes = !inQuotes;
                current.append(c);
            } else if (!inQuotes
                && (c == '{' || c == '[')
            ) {
                depth++;
                current.append(c);
            } else if (!inQuotes
                && (c == '}' || c == ']')
            ) {
                depth--;
                current.append(c);
            } else if (c == ',' && depth == 0
                && !inQuotes
            ) {
                parts.add(current.toString());
                current.setLength(0);
            } else {
                current.append(c);
            }
        }

        if (!current.isEmpty()) {
            parts.add(current.toString());
        }

        return parts.toArray(new String[0]);
    }
}
