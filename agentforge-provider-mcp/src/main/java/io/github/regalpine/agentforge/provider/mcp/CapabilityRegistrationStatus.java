package io.github.regalpine.agentforge.provider.mcp;

/**
 * Capability registration status.
 * AE-10 §25: Capability Registration.
 *
 * <p>Discovery does not equal authorization.
 * Newly discovered tools start as DISCOVERED
 * and must be explicitly enabled.
 */
public enum CapabilityRegistrationStatus {

    DISCOVERED,

    REGISTERED,

    ENABLED,

    DISABLED,

    REVOKED
}
