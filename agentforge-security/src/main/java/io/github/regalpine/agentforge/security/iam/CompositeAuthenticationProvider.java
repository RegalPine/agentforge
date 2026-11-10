package io.github.regalpine.agentforge.security.iam;

import io.github.regalpine.agentforge.security.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Composite authentication provider that delegates to a chain
 * of providers in order.
 * <p>
 * The first provider that returns {@link AuthenticationSuccess} wins.
 * If all providers fail, the last failure is returned.
 * <p>
 * This enables combining built-in and external IAM authentication
 * in a single provider:
 * <pre>
 *   var composite = new CompositeAuthenticationProvider(
 *       builtInProvider,
 *       externalIamProvider
 *   );
 * </pre>
 */
public class CompositeAuthenticationProvider
    implements AuthenticationProvider {

    private final List<AuthenticationProvider> providers;

    public CompositeAuthenticationProvider(
        List<AuthenticationProvider> providers
    ) {
        Objects.requireNonNull(providers);
        this.providers = List.copyOf(providers);
    }

    public CompositeAuthenticationProvider(
        AuthenticationProvider... providers
    ) {
        this(List.of(providers));
    }

    @Override
    public AuthenticationResult authenticate(
        AuthenticationRequest request
    ) {
        AuthenticationResult lastFailure = null;

        for (AuthenticationProvider provider : providers) {
            AuthenticationResult result =
                provider.authenticate(request);

            if (result instanceof AuthenticationSuccess) {
                return result;
            }

            lastFailure = result;
        }

        if (lastFailure != null) {
            return lastFailure;
        }

        return new AuthenticationFailure(
            "AF-IAM-020",
            "No authentication providers configured"
        );
    }

    /**
     * Get the list of delegate providers.
     */
    public List<AuthenticationProvider> providers() {
        return providers;
    }
}
