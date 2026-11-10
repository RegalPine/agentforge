package io.github.regalpine.agentforge.capability;

/**
 * Unified capability error types.
 * AE-02 §43 + AE-10 §49: Error model.
 */
public enum CapabilityErrorType {

    CONNECTION_ERROR,

    AUTHENTICATION_ERROR,

    AUTHORIZATION_ERROR,

    INVALID_ARGUMENT,

    TIMEOUT,

    RATE_LIMITED,

    REMOTE_ERROR,

    PROTOCOL_ERROR,

    SCHEMA_VALIDATION_ERROR,

    POLICY_DENIED,

    CAPABILITY_NOT_FOUND,

    UNKNOWN
}
