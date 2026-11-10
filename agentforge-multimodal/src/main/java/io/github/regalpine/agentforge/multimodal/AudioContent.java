package io.github.regalpine.agentforge.multimodal;

import java.util.Map;
import java.util.Objects;

/**
 * Audio content.
 * AF-ENHANCE-13 §9: AudioContent.
 */
public record AudioContent(
    String artifactReference,
    String codec,
    int sampleRate,
    int channels,
    long durationMs,
    String mimeType,
    Map<String, Object> metadata
) implements ContentPart {

    public AudioContent {
        Objects.requireNonNull(
            artifactReference,
            "artifactReference"
        );

        codec = codec == null
            ? "" : codec;
        mimeType = mimeType == null
            ? "audio/wav" : mimeType;
        metadata = metadata == null
            ? Map.of() : Map.copyOf(metadata);
    }

    @Override
    public ContentType type() {
        return ContentType.AUDIO;
    }

    @Override
    public Object value() {
        return artifactReference;
    }
}
