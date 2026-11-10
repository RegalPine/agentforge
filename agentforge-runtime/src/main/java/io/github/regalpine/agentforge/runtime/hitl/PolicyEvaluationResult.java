package io.github.regalpine.agentforge.runtime.hitl;

/**
 * Policy 评估结果。
 *
 * <p>对应 AF-ENHANCE-03 §10。</p>
 */
public enum PolicyEvaluationResult {

    /**
     * 允许执行。
     */
    ALLOW,

    /**
     * 拒绝执行。
     */
    DENY,

    /**
     * 需要人工审批。
     */
    APPROVAL_REQUIRED
}
