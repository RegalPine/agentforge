package io.github.regalpine.agentforge.provider.spi;

public enum ProviderFailureCategory {

    VALIDATION,
    AUTHENTICATION,
    AUTHORIZATION,
    TIMEOUT,
    CANCELLED,
    NETWORK,
    RATE_LIMIT,
    UNAVAILABLE,
    EXTERNAL,
    PROVIDER,
    UNKNOWN
}
