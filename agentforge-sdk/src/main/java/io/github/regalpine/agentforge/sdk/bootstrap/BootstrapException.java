package io.github.regalpine.agentforge.sdk.bootstrap;

import java.util.Objects;

/**
 * Exception thrown during bootstrap
 * lifecycle transitions.
 * AF-ENHANCE-01 §19-20.
 */
public class BootstrapException
    extends RuntimeException {

    private final BootstrapState from;
    private final BootstrapState to;

    public BootstrapException(
        String message,
        BootstrapState from,
        BootstrapState to
    ) {
        super(message);
        this.from = from;
        this.to = to;
    }

    public BootstrapException(
        String message
    ) {
        super(message);
        this.from = null;
        this.to = null;
    }

    public BootstrapException(
        String message,
        Throwable cause
    ) {
        super(message, cause);
        this.from = null;
        this.to = null;
    }

    /**
     * The source state (null if
     * not a transition error).
     */
    public BootstrapState from() {
        return from;
    }

    /**
     * The target state (null if
     * not a transition error).
     */
    public BootstrapState to() {
        return to;
    }
}
