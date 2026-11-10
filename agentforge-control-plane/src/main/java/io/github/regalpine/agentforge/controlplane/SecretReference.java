package io.github.regalpine.agentforge.controlplane;

/**
 * Secret reference.
 * AF-ENHANCE-14 §21: Secret.
 *
 * <p>Secrets can only be referenced,
 * never stored in plain text in
 * Configuration or Agent Definition.
 */
public record SecretReference(
    String provider,
    String key
) {

    public SecretReference {
        if (provider == null
            || provider.isBlank()) {
            throw new IllegalArgumentException(
                "provider required");
        }
        if (key == null
            || key.isBlank()) {
            throw new IllegalArgumentException(
                "key required");
        }
    }

    /**
     * Create reference.
     */
    public static SecretReference of(
        String provider,
        String key
    ) {
        return new SecretReference(
            provider, key);
    }
}
