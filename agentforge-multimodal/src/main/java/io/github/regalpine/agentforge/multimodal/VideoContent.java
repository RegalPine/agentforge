package io.github.regalpine.agentforge.multimodal;

import java.util.Map;
import java.util.Objects;

/**
 * Video content.
 * AF-ENHANCE-13 §10: VideoContent.
 */
public record VideoContent(
    String artifactReference,
    String codec,
    int width,
    int height,
    double frameRate,
    long durationMs,
    String mimeType,
    Map<String, Object> metadata
) implements ContentPart {

    public VideoContent {
        Objects.requireNonNull(
            artifactReference,
            "artifactReference"
        );

        codec = codec == null
            ? "" : codec;
        mimeType = mimeType == null
            ? "video/mp4" : mimeType;
        metadata = metadata == null
            ? Map.of() : Map.copyOf(metadata);
    }

    @Override
    public ContentType type() {
        return ContentType.VIDEO;
    }

    @Override
    public Object value() {
        return artifactReference;
    }
}
