package io.github.regalpine.agentforge.multimodal;

import java.util.Map;
import java.util.Objects;

/**
 * File content.
 * AF-ENHANCE-13 §11: FileContent.
 *
 * <p>Uses artifact reference.
 * Binary data should not be
 * embedded directly in messages.
 */
public record FileContent(
    String artifactReference,
    String fileName,
    String contentType,
    long size,
    Map<String, Object> metadata
) implements ContentPart {

    public FileContent {
        Objects.requireNonNull(
            artifactReference,
            "artifactReference"
        );

        fileName = fileName == null
            ? "" : fileName;
        contentType = contentType == null
            ? "application/octet-stream"
            : contentType;
        metadata = metadata == null
            ? Map.of() : Map.copyOf(metadata);
    }

    @Override
    public ContentType type() {
        return ContentType.FILE;
    }

    @Override
    public Object value() {
        return artifactReference;
    }
}
