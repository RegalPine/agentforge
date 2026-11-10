package io.github.regalpine.agentforge.multiagent.event;

/**
 * DelegationEventType — AE-11 §38.
 *
 * Multi-agent collaboration event types.
 */
public enum DelegationEventType {

    AGENT_DELEGATION_REQUESTED,
    AGENT_DELEGATION_ACCEPTED,
    AGENT_DELEGATION_REJECTED,
    AGENT_EXECUTION_CREATED,
    AGENT_EXECUTION_STARTED,
    AGENT_EXECUTION_COMPLETED,
    AGENT_EXECUTION_FAILED,
    AGENT_RESULT_RECEIVED,
    AGENT_DELEGATION_CANCELLED,
    AGENT_CIRCULAR_DETECTION
}
