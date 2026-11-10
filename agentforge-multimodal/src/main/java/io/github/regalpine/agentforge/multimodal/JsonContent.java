package io.github.regalpine.agentforge.multimodal;

import java.util.Map;
import java.util.Objects;

/**
 * JSON / structured content.
 * AF-ENHANCE-13 §12: JsonContent.
 */
public record JsonContent(
    Object value,
    Map<String, Object> metadata
) implements ContentPart {

    public JsonContent {
        Objects.requireNonNull(
            value, "value"
        );
        metadata = metadata == null
            ? Map.of() : Map.copyOf(metadata);
    }

    /**
     * Create JSON content.
     */
    public static JsonContent of(
        Object value
    ) {
        return new JsonContent(
            value, Map.of()
        );
    }

    @Override
    public ContentType type() {
        return ContentType.JSON;
    }
}
