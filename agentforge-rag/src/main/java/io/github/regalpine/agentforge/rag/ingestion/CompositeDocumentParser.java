package io.github.regalpine.agentforge.rag.ingestion;

import java.util.List;

/**
 * Composite document parser that delegates to registered parsers.
 * AE-09 §14: Document Parser SPI.
 */
public final class CompositeDocumentParser implements DocumentParser {

    private final List<DocumentParser> parsers;

    public CompositeDocumentParser(List<DocumentParser> parsers) {
        this.parsers = List.copyOf(parsers);
    }

    public static CompositeDocumentParser defaults() {
        return new CompositeDocumentParser(List.of(
                new MarkdownParser(),
                new HtmlParser(),
                new CsvParser(),
                new JsonParser(),
                new PdfParser(),
                new TextParser()
        ));
    }

    @Override
    public ParsedDocument parse(String documentId, String mimeType, String content) {
        for (DocumentParser parser : parsers) {
            if (parser.supports(mimeType)) {
                return parser.parse(documentId, mimeType, content);
            }
        }
        return new TextParser().parse(documentId, mimeType, content);
    }

    @Override
    public boolean supports(String mimeType) {
        return true;
    }
}
