package io.github.regalpine.agentforge.model;

/**
 * Exception thrown when structured output does not conform to the schema.
 * <p>
 * This can occur when:
 * <ul>
 *   <li>The model output cannot be parsed</li>
 *   <li>The parsed output does not match the schema</li>
 *   <li>All repair attempts have been exhausted</li>
 * </ul>
 */
public class StructuredOutputException extends ModelException {

    private final String rawContent;
    private final String schema;
    private final int attemptsMade;

    public StructuredOutputException(String message) {
        super(message);
        this.rawContent = null;
        this.schema = null;
        this.attemptsMade = 0;
    }

    public StructuredOutputException(String message, Throwable cause) {
        super(message, cause);
        this.rawContent = null;
        this.schema = null;
        this.attemptsMade = 0;
    }

    public StructuredOutputException(String message, String rawContent, String schema, int attemptsMade) {
        super(message);
        this.rawContent = rawContent;
        this.schema = schema;
        this.attemptsMade = attemptsMade;
    }

    /**
     * Get the raw content that failed validation.
     *
     * @return the raw content, or null if not available
     */
    public String rawContent() {
        return rawContent;
    }

    /**
     * Get the schema that was expected.
     *
     * @return the schema, or null if not available
     */
    public String schema() {
        return schema;
    }

    /**
     * Get the number of repair attempts made.
     *
     * @return the number of attempts
     */
    public int attemptsMade() {
        return attemptsMade;
    }
}
