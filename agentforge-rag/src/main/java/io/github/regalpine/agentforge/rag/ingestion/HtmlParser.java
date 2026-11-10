package io.github.regalpine.agentforge.rag.ingestion;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * HTML parser.
 * AE-09 §14-16: Document Parser with structure preservation.
 *
 * <p>Extracts headings, paragraphs, tables, and lists from HTML content
 * by stripping tags and preserving structure.
 */
public final class HtmlParser implements DocumentParser {

    private static final Pattern TAG = Pattern.compile("<[^>]+>");
    private static final Pattern HEADING_TAG = Pattern.compile("<h([1-6])[^>]*>(.*?)</h\\1>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
    private static final Pattern TITLE_TAG = Pattern.compile("<title[^>]*>(.*?)</title>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);

    @Override
    public ParsedDocument parse(String documentId, String mimeType, String content) {
        List<DocumentElement> elements = new ArrayList<>();
        String title = "";

        Matcher titleMatcher = TITLE_TAG.matcher(content);
        if (titleMatcher.find()) {
            title = stripTags(titleMatcher.group(1)).trim();
        }

        Matcher headingMatcher = HEADING_TAG.matcher(content);
        while (headingMatcher.find()) {
            int level = Integer.parseInt(headingMatcher.group(1));
            String text = stripTags(headingMatcher.group(2)).trim();
            elements.add(DocumentElement.heading(text, level));
        }

        String[] paragraphs = content.split("(?i)</p>");
        for (String para : paragraphs) {
            String text = stripTags(para).trim();
            if (!text.isEmpty() && text.length() > 10) {
                elements.add(DocumentElement.paragraph(text));
            }
        }

        if (title.isEmpty()) {
            for (DocumentElement el : elements) {
                if (el.type() == DocumentElementType.HEADING) {
                    title = el.content();
                    break;
                }
            }
        }
        if (title.isEmpty()) {
            String stripped = stripTags(content).trim();
            title = stripped.length() > 100 ? stripped.substring(0, 100) : stripped;
        }

        String fullText = stripTags(content).trim();
        return new ParsedDocument(documentId, title, fullText, elements, Map.of("format", "html"));
    }

    @Override
    public boolean supports(String mimeType) {
        return "text/html".equalsIgnoreCase(mimeType)
                || "application/xhtml+xml".equalsIgnoreCase(mimeType);
    }

    private String stripTags(String html) {
        return TAG.matcher(html).replaceAll(" ").replaceAll("\\s+", " ").trim();
    }
}
