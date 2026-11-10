package io.github.regalpine.agentforge.runtime.hitl;

/**
 * 审批主体类型。
 *
 * <p>对应 AF-ENHANCE-03 §16。</p>
 */
public enum ApprovalPrincipalType {

    USER,

    ROLE,

    GROUP,

    SERVICE,

    EXTERNAL_IDP_GROUP
}
