package io.github.regalpine.agentforge.observability;

public enum FailureCategory {

    VALIDATION,
    AUTHENTICATION,
    AUTHORIZATION,
    REFERENCE,
    COMPILATION,
    SCHEDULING,
    QUEUE,
    LEASE,
    FENCING,
    STATE_CONFLICT,
    PERSISTENCE,
    CAPABILITY,
    PROVIDER,
    TIMEOUT,
    CANCELLATION,
    NETWORK,
    EXTERNAL,
    UNKNOWN
}
