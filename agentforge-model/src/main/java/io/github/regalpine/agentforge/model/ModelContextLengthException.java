package io.github.regalpine.agentforge.model;

/**
 * Exception thrown when the context length is exceeded.
 * <p>
 * This can occur when:
 * <ul>
 *   <li>The input is too long for the model</li>
 *   <li>The combined input + output exceeds the model's limit</li>
 * </ul>
 */
public class ModelContextLengthException extends ModelException {

    private final Integer maxTokens;
    private final Integer actualTokens;

    public ModelContextLengthException(String message) {
        super(message);
        this.maxTokens = null;
        this.actualTokens = null;
    }

    public ModelContextLengthException(String message, String modelId, String providerId) {
        super(message, modelId, providerId);
        this.maxTokens = null;
        this.actualTokens = null;
    }

    public ModelContextLengthException(String message, String modelId, String providerId, Integer maxTokens, Integer actualTokens) {
        super(message, modelId, providerId);
        this.maxTokens = maxTokens;
        this.actualTokens = actualTokens;
    }

    public ModelContextLengthException(String message, Throwable cause) {
        super(message, cause);
        this.maxTokens = null;
        this.actualTokens = null;
    }

    /**
     * Get the maximum allowed tokens.
     *
     * @return the maximum tokens, or null if not specified
     */
    public Integer maxTokens() {
        return maxTokens;
    }

    /**
     * Get the actual number of tokens used.
     *
     * @return the actual tokens, or null if not specified
     */
    public Integer actualTokens() {
        return actualTokens;
    }
}
