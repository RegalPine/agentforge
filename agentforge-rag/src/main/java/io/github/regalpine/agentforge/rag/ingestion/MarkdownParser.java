package io.github.regalpine.agentforge.rag.ingestion;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Markdown parser.
 * AE-09 §14-16: Document Parser with structure preservation.
 *
 * <p>Extracts headings, paragraphs, code blocks, lists, and tables
 * from Markdown content. Preserves structure for chunking.
 */
public final class MarkdownParser implements DocumentParser {

    private static final Pattern HEADING = Pattern.compile("^(#{1,6})\\s+(.+)$", Pattern.MULTILINE);
    private static final Pattern CODE_BLOCK = Pattern.compile("^```[\\s\\S]*?^```", Pattern.MULTILINE);
    private static final Pattern TABLE_ROW = Pattern.compile("^\\|.+\\|$", Pattern.MULTILINE);

    @Override
    public ParsedDocument parse(String documentId, String mimeType, String content) {
        List<DocumentElement> elements = new ArrayList<>();
        String title = "";

        String[] lines = content.split("\n");
        StringBuilder currentParagraph = new StringBuilder();
        boolean inCodeBlock = false;
        StringBuilder codeContent = new StringBuilder();

        for (String line : lines) {
            if (line.startsWith("```")) {
                if (inCodeBlock) {
                    elements.add(DocumentElement.code(codeContent.toString().trim()));
                    codeContent.setLength(0);
                    inCodeBlock = false;
                } else {
                    flushParagraph(currentParagraph, elements);
                    inCodeBlock = true;
                }
                continue;
            }

            if (inCodeBlock) {
                codeContent.append(line).append("\n");
                continue;
            }

            Matcher headingMatcher = HEADING.matcher(line);
            if (headingMatcher.matches()) {
                flushParagraph(currentParagraph, elements);
                int level = headingMatcher.group(1).length();
                String text = headingMatcher.group(2).trim();
                elements.add(DocumentElement.heading(text, level));
                if (level == 1 && title.isEmpty()) {
                    title = text;
                }
                continue;
            }

            if (TABLE_ROW.matcher(line).matches()) {
                flushParagraph(currentParagraph, elements);
                elements.add(DocumentElement.table(line));
                continue;
            }

            if (line.startsWith("- ") || line.startsWith("* ") || line.matches("^\\d+\\.\\s.*")) {
                flushParagraph(currentParagraph, elements);
                elements.add(DocumentElement.list(line));
                continue;
            }

            if (line.isBlank()) {
                flushParagraph(currentParagraph, elements);
            } else {
                if (!currentParagraph.isEmpty()) {
                    currentParagraph.append(" ");
                }
                currentParagraph.append(line.trim());
            }
        }

        flushParagraph(currentParagraph, elements);

        if (title.isEmpty() && !elements.isEmpty()) {
            for (DocumentElement el : elements) {
                if (el.type() == DocumentElementType.HEADING) {
                    title = el.content();
                    break;
                }
            }
        }
        if (title.isEmpty()) {
            title = content.length() > 100 ? content.substring(0, 100) : content;
        }

        return new ParsedDocument(documentId, title, content, elements, Map.of("format", "markdown"));
    }

    @Override
    public boolean supports(String mimeType) {
        return "text/markdown".equalsIgnoreCase(mimeType)
                || "text/x-markdown".equalsIgnoreCase(mimeType)
                || mimeType.endsWith(".md");
    }

    private void flushParagraph(StringBuilder buffer, List<DocumentElement> elements) {
        if (!buffer.isEmpty()) {
            elements.add(DocumentElement.paragraph(buffer.toString().trim()));
            buffer.setLength(0);
        }
    }
}
