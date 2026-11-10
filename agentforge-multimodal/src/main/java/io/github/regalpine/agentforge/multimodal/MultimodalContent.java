package io.github.regalpine.agentforge.multimodal;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Multimodal content aggregate.
 * AF-ENHANCE-13 §13-14: Multimodal
 * Message.
 *
 * <p>A single message can contain
 * mixed content: text + image +
 * file, etc.
 */
public record MultimodalContent(
    List<ContentPart> parts,
    Map<String, Object> metadata
) {

    public MultimodalContent {
        Objects.requireNonNull(
            parts, "parts"
        );

        parts = List.copyOf(parts);
        metadata = metadata == null
            ? Map.of() : Map.copyOf(metadata);
    }

    /**
     * Create from a single part.
     */
    public static MultimodalContent of(
        ContentPart part
    ) {
        return new MultimodalContent(
            List.of(part), Map.of()
        );
    }

    /**
     * Create from multiple parts.
     */
    public static MultimodalContent of(
        ContentPart... parts
    ) {
        return new MultimodalContent(
            List.of(parts), Map.of()
        );
    }

    /**
     * Create text-only content.
     */
    public static MultimodalContent text(
        String text
    ) {
        return of(
            TextContent.of(text)
        );
    }

    /**
     * Check if content is empty.
     */
    public boolean isEmpty() {
        return parts.isEmpty();
    }

    /**
     * Get the number of parts.
     */
    public int size() {
        return parts.size();
    }

    /**
     * Check if content contains
     * a specific type.
     */
    public boolean containsType(
        ContentType type
    ) {
        return parts.stream()
            .anyMatch(p ->
                p.type() == type
            );
    }
}
