package io.github.regalpine.agentforge.runtime.hitl;

/**
 * 审批状态。
 *
 * <p>对应 AF-ENHANCE-03 §12。</p>
 */
public enum ApprovalStatus {

    PENDING,

    APPROVED,

    REJECTED,

    MODIFIED,

    ABORTED,

    EXPIRED,

    ESCALATED,

    CANCELLED;

    /**
     * 是否为终态。
     */
    public boolean isTerminal() {
        return this != PENDING
            && this != ESCALATED;
    }

    /**
     * 是否可接受转换。
     */
    public boolean canTransitionTo(
        ApprovalStatus target
    ) {
        if (isTerminal()) {
            return false;
        }
        return switch (this) {
            case PENDING ->
                target == APPROVED
                || target == REJECTED
                || target == MODIFIED
                || target == ABORTED
                || target == EXPIRED
                || target == ESCALATED
                || target == CANCELLED;
            case ESCALATED ->
                target == APPROVED
                || target == REJECTED
                || target == ABORTED
                || target == EXPIRED
                || target == CANCELLED;
            default -> false;
        };
    }
}
