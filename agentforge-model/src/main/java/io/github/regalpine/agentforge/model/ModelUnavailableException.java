package io.github.regalpine.agentforge.model;

/**
 * Exception thrown when a model is unavailable.
 * <p>
 * This can occur when:
 * <ul>
 *   <li>The model is not registered</li>
 *   <li>The provider is not available</li>
 *   <li>The model cannot be resolved from a reference</li>
 * </ul>
 */
public class ModelUnavailableException extends ModelException {

    public ModelUnavailableException(String message) {
        super(message);
    }

    public ModelUnavailableException(String message, String modelId, String providerId) {
        super(message, modelId, providerId);
    }

    public ModelUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
