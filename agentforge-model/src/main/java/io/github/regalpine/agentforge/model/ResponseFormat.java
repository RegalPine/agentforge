package io.github.regalpine.agentforge.model;

import java.util.Objects;

/**
 * Response format specification for model output.
 */
public record ResponseFormat(
    ResponseFormatType type,
    String schema
) {

    public ResponseFormat {
        Objects.requireNonNull(type, "type");
    }

    public static ResponseFormat text() {
        return new ResponseFormat(ResponseFormatType.TEXT, null);
    }

    public static ResponseFormat json() {
        return new ResponseFormat(ResponseFormatType.JSON, null);
    }

    public static ResponseFormat jsonSchema(String schema) {
        return new ResponseFormat(ResponseFormatType.JSON_SCHEMA, schema);
    }
}
