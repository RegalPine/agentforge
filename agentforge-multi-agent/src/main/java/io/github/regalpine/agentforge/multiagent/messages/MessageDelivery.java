package io.github.regalpine.agentforge.multiagent.messages;

/**
 * MessageDelivery —
 * AF-ENHANCE-08 §34.
 *
 * Delivery semantics for
 * agent-to-agent messages.
 */
public enum MessageDelivery {

    /**
     * At most once.
     * May lose messages.
     */
    AT_MOST_ONCE,

    /**
     * At least once.
     * May duplicate.
     * Default.
     */
    AT_LEAST_ONCE
}
