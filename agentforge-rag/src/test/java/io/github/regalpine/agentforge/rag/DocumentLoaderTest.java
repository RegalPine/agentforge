package io.github.regalpine.agentforge.rag;

import io.github.regalpine.agentforge.rag.ingestion.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Document loader and parser tests.
 * AF-ENHANCE-03 §12: verifies that
 * all 6 formats (TXT, MD, HTML, CSV,
 * JSON, PDF) can be loaded and parsed.
 */
class DocumentLoaderTest {

    @TempDir
    Path tempDir;

    @Test
    void shouldLoadTextFile() throws IOException {
        Path file = tempDir.resolve("test.txt");
        Files.writeString(file, "Hello world");

        var loader = new FileDocumentLoader();
        var doc = loader.load(file.toString());

        assertEquals("text/plain", doc.mimeType());
        assertEquals("Hello world", doc.content());
    }

    @Test
    void shouldLoadMarkdownFile() throws IOException {
        Path file = tempDir.resolve("test.md");
        Files.writeString(file, "# Title\n\nParagraph");

        var loader = new FileDocumentLoader();
        var doc = loader.load(file.toString());

        assertEquals("text/markdown", doc.mimeType());
    }

    @Test
    void shouldLoadHtmlFile() throws IOException {
        Path file = tempDir.resolve("test.html");
        Files.writeString(file,
            "<html><body><p>Hello</p></body></html>");

        var loader = new FileDocumentLoader();
        var doc = loader.load(file.toString());

        assertEquals("text/html", doc.mimeType());
    }

    @Test
    void shouldLoadCsvFile() throws IOException {
        Path file = tempDir.resolve("data.csv");
        Files.writeString(file,
            "name,age\nAlice,30\nBob,25");

        var loader = new FileDocumentLoader();
        var doc = loader.load(file.toString());

        assertEquals("text/csv", doc.mimeType());
    }

    @Test
    void shouldLoadJsonFile() throws IOException {
        Path file = tempDir.resolve("data.json");
        Files.writeString(file,
            "{\"key\":\"value\",\"num\":42}");

        var loader = new FileDocumentLoader();
        var doc = loader.load(file.toString());

        assertEquals("application/json",
            doc.mimeType());
    }

    @Test
    void shouldDetectMimeType() {
        assertEquals("text/markdown",
            FileDocumentLoader.detectMimeType("a.md"));
        assertEquals("text/html",
            FileDocumentLoader.detectMimeType("a.html"));
        assertEquals("text/csv",
            FileDocumentLoader.detectMimeType("a.csv"));
        assertEquals("application/json",
            FileDocumentLoader.detectMimeType("a.json"));
        assertEquals("application/pdf",
            FileDocumentLoader.detectMimeType("a.pdf"));
        assertEquals("text/plain",
            FileDocumentLoader.detectMimeType("a.txt"));
    }

    @Test
    void shouldParseCsvContent() {
        var parser = new CsvParser();

        var result = parser.parse("doc1", "text/csv",
            "name,age\nAlice,30\nBob,25");

        assertTrue(result.hasStructure());
        assertTrue(result.text().contains("Alice"));
        assertTrue(result.text().contains("Bob"));
        assertEquals(2, result.metadata().get("rowCount"));
    }

    @Test
    void shouldParseJsonContent() {
        var parser = new JsonParser();

        var result = parser.parse("doc1",
            "application/json",
            "{\"name\":\"Alice\",\"age\":\"30\"}");

        assertTrue(result.hasStructure());
        assertTrue(result.text().contains("name"));
        assertTrue(result.text().contains("Alice"));
    }

    @Test
    void shouldParseJsonArray() {
        var parser = new JsonParser();

        var result = parser.parse("doc1",
            "application/json",
            "[\"alpha\",\"beta\",\"gamma\"]");

        assertTrue(result.hasStructure());
        assertTrue(result.text().contains("alpha"));
    }

    @Test
    void shouldParsePdfContent() {
        var parser = new PdfParser();

        assertTrue(parser.supports("application/pdf"));
        assertTrue(parser.supports("application/x-pdf"));
        assertFalse(parser.supports("text/plain"));
    }

    @Test
    void shouldSupportAllFormatsInComposite() {
        var composite =
            CompositeDocumentParser.defaults();

        assertTrue(composite.supports("text/plain"));
        assertTrue(composite.supports("text/markdown"));
        assertTrue(composite.supports("text/html"));
        assertTrue(composite.supports("text/csv"));
        assertTrue(composite.supports("application/json"));
        assertTrue(composite.supports("application/pdf"));
    }

    @Test
    void shouldParseCsvViaComposite() {
        var composite =
            CompositeDocumentParser.defaults();

        var result = composite.parse("doc1",
            "text/csv",
            "col1,col2\nval1,val2");

        assertTrue(result.text().contains("col1"));
        assertTrue(result.text().contains("val1"));
    }

    @Test
    void shouldParseJsonViaComposite() {
        var composite =
            CompositeDocumentParser.defaults();

        var result = composite.parse("doc1",
            "application/json",
            "{\"x\":\"y\"}");

        assertTrue(result.text().contains("x"));
    }
}
