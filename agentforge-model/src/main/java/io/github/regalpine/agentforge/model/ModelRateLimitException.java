package io.github.regalpine.agentforge.model;

/**
 * Exception thrown when a model rate limit is exceeded.
 * <p>
 * This can occur when:
 * <ul>
 *   <li>Too many requests in a short time</li>
 *   <li>Token quota exceeded</li>
 *   <li>Provider-imposed limits reached</li>
 * </ul>
 */
public class ModelRateLimitException extends ModelException {

    private final Long retryAfterSeconds;

    public ModelRateLimitException(String message) {
        super(message);
        this.retryAfterSeconds = null;
    }

    public ModelRateLimitException(String message, String modelId, String providerId) {
        super(message, modelId, providerId);
        this.retryAfterSeconds = null;
    }

    public ModelRateLimitException(String message, String modelId, String providerId, Long retryAfterSeconds) {
        super(message, modelId, providerId);
        this.retryAfterSeconds = retryAfterSeconds;
    }

    public ModelRateLimitException(String message, Throwable cause) {
        super(message, cause);
        this.retryAfterSeconds = null;
    }

    /**
     * Get the recommended retry delay.
     *
     * @return the number of seconds to wait before retrying, or null if not specified
     */
    public Long retryAfterSeconds() {
        return retryAfterSeconds;
    }
}
