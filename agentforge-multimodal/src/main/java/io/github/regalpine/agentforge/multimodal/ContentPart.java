package io.github.regalpine.agentforge.multimodal;

import java.util.Map;

/**
 * Content part interface.
 * AF-ENHANCE-13 §5: ContentPart.
 *
 * <p>Unified abstraction for all
 * content types (text, image,
 * audio, video, file, json,
 * binary).
 */
public interface ContentPart {

    /**
     * Content type.
     */
    ContentType type();

    /**
     * Content metadata.
     */
    Map<String, Object> metadata();

    /**
     * Content value.
     */
    Object value();
}
