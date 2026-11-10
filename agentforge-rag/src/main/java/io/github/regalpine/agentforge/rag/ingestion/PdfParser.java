package io.github.regalpine.agentforge.rag.ingestion;

import java.util.List;
import java.util.Map;

/**
 * PDF document parser (basic text extraction).
 * AF-ENHANCE-03 §12: extracts text from
 * PDF content. This is a basic implementation
 * that handles the text content stream.
 *
 * <p>For production use, integrate with
 * Apache PDFBox or similar library.
 */
public final class PdfParser
    implements DocumentParser {

    @Override
    public ParsedDocument parse(
        String documentId,
        String mimeType,
        String content
    ) {
        // Basic PDF text extraction.
        // Extracts text between BT/ET markers
        // and Tj/TJ operators.
        StringBuilder textContent =
            new StringBuilder();

        String[] lines = content.split("\n");

        boolean inTextBlock = false;

        for (String line : lines) {
            String trimmed = line.trim();

            if (trimmed.equals("BT")) {
                inTextBlock = true;
                continue;
            }

            if (trimmed.equals("ET")) {
                inTextBlock = false;
                continue;
            }

            if (inTextBlock) {
                // Extract text from Tj operator
                if (trimmed.endsWith("Tj")) {
                    String text = extractQuotedText(
                        trimmed
                    );
                    if (!text.isEmpty()) {
                        textContent.append(text)
                                   .append(" ");
                    }
                }
                // Extract text from TJ operator
                if (trimmed.endsWith("TJ")) {
                    String text = extractTjText(
                        trimmed
                    );
                    if (!text.isEmpty()) {
                        textContent.append(text)
                                   .append(" ");
                    }
                }
            }
        }

        String result = textContent.toString()
            .trim();

        if (result.isEmpty()) {
            // Fallback: treat as plain text
            result = content;
        }

        return new ParsedDocument(
            documentId, "",
            result,
            List.of(
                DocumentElement.paragraph(result)
            ),
            Map.of("format", "pdf")
        );
    }

    @Override
    public boolean supports(String mimeType) {
        return "application/pdf".equals(mimeType)
            || "application/x-pdf".equals(mimeType);
    }

    private String extractQuotedText(String line) {
        int start = line.indexOf('(');
        int end = line.lastIndexOf(')');

        if (start >= 0 && end > start) {
            return line.substring(start + 1, end);
        }

        return "";
    }

    private String extractTjText(String line) {
        StringBuilder sb = new StringBuilder();
        boolean inParen = false;

        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);

            if (c == '(') {
                inParen = true;
            } else if (c == ')') {
                inParen = false;
            } else if (inParen) {
                sb.append(c);
            }
        }

        return sb.toString();
    }
}
