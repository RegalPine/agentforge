package io.github.regalpine.agentforge.capability;

/**
 * Capability lifecycle status.
 * AE-02 §22: Capability 生命周期.
 *
 * <p>The runtime must reject invocations on
 * capabilities in DISABLED or REMOVED status.
 */
public enum CapabilityStatus {

    REGISTERED,

    ACTIVE,

    DEPRECATED,

    DISABLED,

    REMOVED
}
