package io.github.regalpine.agentforge.workspace;

import java.util.Set;

/**
 * Network policy for sandbox.
 * AF-ENHANCE-07 §36: Network Policy.
 *
 * <p>Default: DENY.
 */
public enum NetworkMode {

    DENY,

    ALLOWLIST,

    FULL
}
