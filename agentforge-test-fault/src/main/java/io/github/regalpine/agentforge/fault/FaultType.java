package io.github.regalpine.agentforge.fault;

/**
 * Types of faults that can be injected.
 */
public enum FaultType {
    /** Throw a specified exception */
    EXCEPTION,
    /** Introduce a delay before proceeding */
    DELAY,
    /** Simulate a timeout (delay longer than caller's timeout) */
    TIMEOUT,
    /** Return a failure result */
    FAILURE,
    /** Drop the request silently (no response) */
    DROP,
    /** Corrupt the data */
    CORRUPT
}
