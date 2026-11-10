package io.github.regalpine.agentforge.runtime.hitl;

/**
 * 暂停原因。
 *
 * <p>对应 AF-ENHANCE-03 §29。</p>
 */
public enum PauseReason {

    HUMAN_APPROVAL,

    HUMAN_INPUT,

    POLICY_REVIEW,

    SYSTEM,

    RESOURCE,

    MANUAL
}
