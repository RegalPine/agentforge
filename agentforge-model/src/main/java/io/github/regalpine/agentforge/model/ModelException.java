package io.github.regalpine.agentforge.model;

/**
 * Base exception for model-related errors.
 * <p>
 * Model exceptions indicate failures during model invocation,
 * including network errors, rate limiting, authentication failures,
 * and other provider-specific issues.
 */
public class ModelException extends RuntimeException {

    private final String modelId;
    private final String providerId;

    public ModelException(String message) {
        super(message);
        this.modelId = null;
        this.providerId = null;
    }

    public ModelException(String message, Throwable cause) {
        super(message, cause);
        this.modelId = null;
        this.providerId = null;
    }

    public ModelException(String message, String modelId, String providerId) {
        super(message);
        this.modelId = modelId;
        this.providerId = providerId;
    }

    public ModelException(String message, String modelId, String providerId, Throwable cause) {
        super(message, cause);
        this.modelId = modelId;
        this.providerId = providerId;
    }

    /**
     * Get the model ID that caused this exception.
     *
     * @return the model ID, or null if not available
     */
    public String modelId() {
        return modelId;
    }

    /**
     * Get the provider ID that caused this exception.
     *
     * @return the provider ID, or null if not available
     */
    public String providerId() {
        return providerId;
    }
}
