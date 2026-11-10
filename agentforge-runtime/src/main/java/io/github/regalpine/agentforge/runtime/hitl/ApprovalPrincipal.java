package io.github.regalpine.agentforge.runtime.hitl;

import java.util.Objects;

/**
 * 审批主体。
 *
 * <p>定义需要什么样的人做决定。
 * 对应 AF-ENHANCE-03 §15-16。</p>
 */
public record ApprovalPrincipal(
    ApprovalPrincipalType type,
    String value
) {

    public ApprovalPrincipal {
        Objects.requireNonNull(
            type, "type"
        );
        Objects.requireNonNull(
            value, "value"
        );
    }

    public static ApprovalPrincipal user(
        String userId
    ) {
        return new ApprovalPrincipal(
            ApprovalPrincipalType.USER,
            userId
        );
    }

    public static ApprovalPrincipal role(
        String role
    ) {
        return new ApprovalPrincipal(
            ApprovalPrincipalType.ROLE,
            role
        );
    }

    public static ApprovalPrincipal group(
        String group
    ) {
        return new ApprovalPrincipal(
            ApprovalPrincipalType.GROUP,
            group
        );
    }

    public static ApprovalPrincipal
    service(
        String serviceId
    ) {
        return new ApprovalPrincipal(
            ApprovalPrincipalType.SERVICE,
            serviceId
        );
    }
}
