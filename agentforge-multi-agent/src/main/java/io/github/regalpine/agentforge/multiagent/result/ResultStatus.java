package io.github.regalpine.agentforge.multiagent.result;

/**
 * ResultStatus — AE-11 §27.
 *
 * Possible statuses for an agent delegation result.
 */
public enum ResultStatus {

    SUCCESS,
    PARTIAL,
    FAILED,
    TIMEOUT,
    CANCELLED,
    WAITING_HUMAN,
    POLICY_DENIED,
    BUDGET_EXCEEDED
}
