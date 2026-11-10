package io.github.regalpine.agentforge.runtime.hitl;

import java.util.Objects;

/**
 * 审批执行者。
 *
 * <p>标识审批决策的发起人。
 * 对应 AF-ENHANCE-03 §36。</p>
 */
public record ApprovalActor(
    String actorId,
    ApprovalPrincipalType actorType,
    boolean authenticated,
    boolean authorized
) {

    public ApprovalActor {
        Objects.requireNonNull(
            actorId, "actorId"
        );
        Objects.requireNonNull(
            actorType, "actorType"
        );
    }

    public static ApprovalActor of(
        String actorId,
        ApprovalPrincipalType type
    ) {
        return new ApprovalActor(
            actorId, type,
            true, true
        );
    }

    /**
     * 是否为用户类型。
     */
    public boolean isUser() {
        return actorType
            == ApprovalPrincipalType.USER;
    }
}
