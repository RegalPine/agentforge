package io.github.regalpine.agentforge.multimodal;

import java.util.Map;
import java.util.Objects;

/**
 * Text content.
 * AF-ENHANCE-13 §7: TextContent.
 */
public record TextContent(
    String text,
    Map<String, Object> metadata
) implements ContentPart {

    public TextContent {
        Objects.requireNonNull(
            text, "text"
        );
        metadata = metadata == null
            ? Map.of() : Map.copyOf(metadata);
    }

    /**
     * Create text content.
     */
    public static TextContent of(
        String text
    ) {
        return new TextContent(
            text, Map.of()
        );
    }

    @Override
    public ContentType type() {
        return ContentType.TEXT;
    }

    @Override
    public Object value() {
        return text;
    }
}
