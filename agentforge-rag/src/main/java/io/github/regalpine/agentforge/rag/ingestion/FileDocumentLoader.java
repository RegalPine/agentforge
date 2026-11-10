package io.github.regalpine.agentforge.rag.ingestion;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

/**
 * File-based document loader.
 * AF-ENHANCE-03 §12: loads documents
 * from the local file system.
 *
 * <p>Detects MIME type from the file
 * extension. Supports TXT, MD, HTML,
 * CSV, JSON and PDF.
 */
public final class FileDocumentLoader
    implements DocumentLoader {

    @Override
    public LoadedDocument load(String source) {
        Path path = Path.of(source);

        if (!Files.exists(path)) {
            throw new DocumentLoadException(
                "File not found: " + source
            );
        }

        if (!Files.isReadable(path)) {
            throw new DocumentLoadException(
                "File not readable: " + source
            );
        }

        try {
            String content = Files.readString(
                path, StandardCharsets.UTF_8
            );

            String mimeType = detectMimeType(
                path.getFileName().toString()
            );

            long size = Files.size(path);

            Map<String, Object> metadata = Map.of(
                "fileName",
                    path.getFileName().toString(),
                "filePath",
                    path.toAbsolutePath().toString(),
                "fileSize", size,
                "lastModified",
                    Files.getLastModifiedTime(path)
                        .toInstant()
                        .toString()
            );

            String docId = "doc-"
                + path.getFileName()
                    .toString()
                    .hashCode();

            return new LoadedDocument(
                docId, mimeType, content, metadata
            );

        } catch (IOException e) {
            throw new DocumentLoadException(
                "Failed to load file: " + source, e
            );
        }
    }

    @Override
    public LoadedDocument load(
        String source,
        InputStream stream,
        String mimeType
    ) {
        try {
            String content = new String(
                stream.readAllBytes(),
                StandardCharsets.UTF_8
            );

            String effectiveMime = mimeType == null
                ? "text/plain"
                : mimeType;

            String docId = "doc-"
                + source.hashCode();

            return new LoadedDocument(
                docId, effectiveMime, content,
                Map.of("source", source)
            );

        } catch (IOException e) {
            throw new DocumentLoadException(
                "Failed to load stream: " + source, e
            );
        }
    }

    @Override
    public boolean supports(String source) {
        return source != null
            && Files.exists(Path.of(source));
    }

    public static String detectMimeType(String fileName) {
        String lower = fileName.toLowerCase();

        if (lower.endsWith(".md")
            || lower.endsWith(".markdown")
        ) {
            return "text/markdown";
        }
        if (lower.endsWith(".html")
            || lower.endsWith(".htm")
        ) {
            return "text/html";
        }
        if (lower.endsWith(".csv")) {
            return "text/csv";
        }
        if (lower.endsWith(".json")) {
            return "application/json";
        }
        if (lower.endsWith(".pdf")) {
            return "application/pdf";
        }
        if (lower.endsWith(".txt")) {
            return "text/plain";
        }

        return "text/plain";
    }

    /**
     * Document loading exception.
     */
    public static final class DocumentLoadException
        extends RuntimeException {

        public DocumentLoadException(String msg) {
            super(msg);
        }

        public DocumentLoadException(
            String msg, Throwable cause
        ) {
            super(msg, cause);
        }
    }
}
