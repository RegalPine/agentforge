package io.github.regalpine.agentforge.model;

/**
 * Exception for provider-level errors.
 * <p>
 * Provider exceptions indicate failures specific to a provider implementation,
 * such as configuration errors, initialization failures, or protocol violations.
 * This is distinct from {@link ModelException} which covers model invocation errors.
 */
public class ProviderException extends ModelException {

    private final String providerType;

    public ProviderException(String message, String providerId, String providerType) {
        super(message, null, providerId);
        this.providerType = providerType;
    }

    public ProviderException(String message, String providerId, String providerType, Throwable cause) {
        super(message, cause);
        this.providerType = providerType;
    }

    /**
     * Get the provider type that caused this exception.
     *
     * @return the provider type (e.g., "openai", "anthropic", "ollama")
     */
    public String providerType() {
        return providerType;
    }
}
