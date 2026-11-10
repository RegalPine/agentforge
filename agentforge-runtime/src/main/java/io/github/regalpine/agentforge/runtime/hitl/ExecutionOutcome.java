package io.github.regalpine.agentforge.runtime.hitl;

import io.github.regalpine.agentforge.event.Execution;
/**
 * 执行结果。
 *
 * <p>与 Execution Status 分离。
 * 对应 AF-ENHANCE-03 §47。</p>
 */
public enum ExecutionOutcome {

    SUCCESS,

    POLICY_DENIED,

    APPROVAL_REJECTED,

    APPROVAL_EXPIRED,

    HUMAN_ABORTED,

    FAILED,

    CANCELLED
}
