package io.github.regalpine.agentforge.sdk;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Secret provider SPI.
 * AF-ENHANCE-04 §5: resolves secret
 * references from configuration without
 * exposing the actual secret values.
 *
 * <p>Resolution priority:
 * Secret Provider > Environment > Config File
 *
 * <p>Implementations can read from:
 * <ul>
 *   <li>Environment variables</li>
 *   <li>Vault / KMS services</li>
 *   <li>Encrypted config files</li>
 * </ul>
 */
public interface SecretProvider {

    /**
     * Resolve a secret by its reference key.
     *
     * @param secretRef the secret reference
     *                  (e.g. "OPENAI_API_KEY")
     * @return the resolved secret value,
     *         or empty if not found
     */
    Optional<String> resolve(String secretRef);

    /**
     * Check if this provider can resolve
     * the given reference.
     */
    default boolean canResolve(String secretRef) {
        return resolve(secretRef).isPresent();
    }

    /**
     * Create an environment-variable-based
     * secret provider.
     */
    static SecretProvider fromEnvironment() {
        return new EnvironmentSecretProvider();
    }

    /**
     * Create a map-based secret provider
     * for testing.
     */
    static SecretProvider fromMap(
        Map<String, String> secrets
    ) {
        return new MapSecretProvider(secrets);
    }

    /**
     * Chain multiple providers. First match wins.
     */
    static SecretProvider chain(
        SecretProvider... providers
    ) {
        return new ChainedSecretProvider(providers);
    }
}

/**
 * Environment variable secret provider.
 */
class EnvironmentSecretProvider
    implements SecretProvider {

    @Override
    public Optional<String> resolve(String ref) {
        String value = System.getenv(ref);
        return Optional.ofNullable(value);
    }
}

/**
 * Map-based secret provider for testing.
 */
class MapSecretProvider implements SecretProvider {

    private final Map<String, String> secrets;

    MapSecretProvider(Map<String, String> secrets) {
        this.secrets =
            Objects.requireNonNull(secrets, "secrets");
    }

    @Override
    public Optional<String> resolve(String ref) {
        return Optional.ofNullable(secrets.get(ref));
    }
}

/**
 * Chained secret provider.
 */
class ChainedSecretProvider implements SecretProvider {

    private final SecretProvider[] providers;

    ChainedSecretProvider(SecretProvider[] providers) {
        this.providers =
            Objects.requireNonNull(providers, "providers");
    }

    @Override
    public Optional<String> resolve(String ref) {
        for (SecretProvider p : providers) {
            Optional<String> result = p.resolve(ref);
            if (result.isPresent()) {
                return result;
            }
        }
        return Optional.empty();
    }
}
