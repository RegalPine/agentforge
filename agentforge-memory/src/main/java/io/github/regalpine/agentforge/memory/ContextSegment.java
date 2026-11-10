package io.github.regalpine.agentforge.memory;

import java.util.Objects;

/**
 * Context segment.
 * AE-08 §61: Context Injection.
 *
 * <p>A segment of the assembled model
 * context. Each segment has a type,
 * content, priority and source for
 * provenance tracking.
 */
public record ContextSegment(
    String segmentId,
    ContextSegmentType type,
    String content,
    int priority,
    String source,
    int estimatedTokens
) {

    public ContextSegment {
        Objects.requireNonNull(
            segmentId, "segmentId"
        );
        Objects.requireNonNull(
            type, "type"
        );
        Objects.requireNonNull(
            content, "content"
        );

        source =
            source == null ? "" : source;

        if (estimatedTokens <= 0) {
            estimatedTokens =
                estimateTokens(content);
        }
    }

    public ContextSegment(
        String segmentId,
        ContextSegmentType type,
        String content,
        int priority,
        String source
    ) {
        this(
            segmentId, type, content,
            priority, source, 0
        );
    }

    /**
     * Rough token estimation:
     * ~4 characters per token.
     */
    private static int estimateTokens(
        String text
    ) {
        if (text == null || text.isBlank()) {
            return 0;
        }
        return Math.max(
            1,
            text.length() / 4
        );
    }
}
