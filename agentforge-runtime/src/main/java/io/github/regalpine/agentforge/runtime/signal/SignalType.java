package io.github.regalpine.agentforge.runtime.signal;

/**
 * Type of wait signal.
 */
public enum SignalType {
    /** Timer-based: resume after a duration or at a specific time */
    TIMER,
    /** External event: resume when an external event arrives */
    EVENT,
    /** Human task: resume when a human completes a task */
    HUMAN,
    /** Generic signal: resume on named signal */
    NAMED
}
