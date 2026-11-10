package io.github.regalpine.agentforge.runtime.signal;

/**
 * Status of a wait signal.
 */
public enum SignalStatus {
    /** Signal is pending (execution is waiting) */
    PENDING,
    /** Signal was resolved (execution can resume) */
    RESOLVED,
    /** Signal expired (timeout) */
    EXPIRED,
    /** Signal was cancelled */
    CANCELLED
}
