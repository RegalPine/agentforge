package io.github.regalpine.agentforge.multimodal;

import java.util.Map;
import java.util.Objects;

/**
 * Image content.
 * AF-ENHANCE-13 §8: ImageContent.
 *
 * <p>Uses artifact reference
 * instead of embedding binary
 * data directly.
 */
public record ImageContent(
    String artifactReference,
    int width,
    int height,
    String format,
    String mimeType,
    long size,
    Map<String, Object> metadata
) implements ContentPart {

    public ImageContent {
        Objects.requireNonNull(
            artifactReference,
            "artifactReference"
        );

        format = format == null
            ? "" : format;
        mimeType = mimeType == null
            ? "image/png" : mimeType;
        metadata = metadata == null
            ? Map.of() : Map.copyOf(metadata);
    }

    @Override
    public ContentType type() {
        return ContentType.IMAGE;
    }

    @Override
    public Object value() {
        return artifactReference;
    }
}
