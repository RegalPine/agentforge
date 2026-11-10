package io.github.regalpine.agentforge.rag.ingestion;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * CSV document parser.
 * AF-ENHANCE-03 §12: parses CSV content
 * into structured elements. Each row
 * becomes a paragraph element with the
 * header as context.
 */
public final class CsvParser
    implements DocumentParser {

    @Override
    public ParsedDocument parse(
        String documentId,
        String mimeType,
        String content
    ) {
        String[] lines = content.split("\n");

        if (lines.length == 0) {
            return ParsedDocument.plain(
                documentId, content
            );
        }

        String[] headers = parseCsvLine(lines[0]);

        StringBuilder fullText =
            new StringBuilder();
        List<DocumentElement> elements =
            new ArrayList<>();

        // Header element
        elements.add(DocumentElement.heading(
            String.join(", ", headers), 1
        ));

        fullText.append(String.join(", ", headers))
                .append("\n\n");

        // Data rows
        for (int i = 1; i < lines.length; i++) {
            String line = lines[i].trim();

            if (line.isEmpty()) continue;

            String[] values = parseCsvLine(line);

            StringBuilder rowText =
                new StringBuilder();

            for (int j = 0;
                 j < headers.length
                     && j < values.length;
                 j++
            ) {
                if (j > 0) rowText.append("; ");
                rowText.append(headers[j].trim())
                       .append(": ")
                       .append(values[j].trim());
            }

            String rowStr = rowText.toString();

            elements.add(
                DocumentElement.paragraph(rowStr)
            );

            fullText.append(rowStr).append("\n");
        }

        return new ParsedDocument(
            documentId, "",
            fullText.toString(),
            elements,
            Map.of("format", "csv",
                "rowCount", lines.length - 1,
                "columnCount", headers.length
            )
        );
    }

    @Override
    public boolean supports(String mimeType) {
        return "text/csv".equals(mimeType);
    }

    private static String[] parseCsvLine(
        String line
    ) {
        List<String> fields = new ArrayList<>();
        boolean inQuotes = false;
        StringBuilder field = new StringBuilder();

        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);

            if (c == '"') {
                inQuotes = !inQuotes;
            } else if (c == ',' && !inQuotes) {
                fields.add(field.toString());
                field.setLength(0);
            } else {
                field.append(c);
            }
        }

        fields.add(field.toString());

        return fields.toArray(new String[0]);
    }
}
