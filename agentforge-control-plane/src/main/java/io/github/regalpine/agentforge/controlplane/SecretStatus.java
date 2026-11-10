package io.github.regalpine.agentforge.controlplane;

/**
 * Secret lifecycle states.
 * AF-ENHANCE-14 §23: Secret Lifecycle.
 */
public enum SecretStatus {

    REGISTERED,

    ACTIVE,

    ROTATING,

    REVOKED
}
