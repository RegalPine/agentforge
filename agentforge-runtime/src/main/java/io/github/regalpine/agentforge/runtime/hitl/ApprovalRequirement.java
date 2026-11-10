package io.github.regalpine.agentforge.runtime.hitl;

import java.util.Objects;
import java.util.Set;

/**
 * 审批要求。
 *
 * <p>定义审批人、最低通过数、
 * 否决策略等。
 * 对应 AF-ENHANCE-03 §15-19。</p>
 */
public record ApprovalRequirement(
    Set<ApprovalPrincipal> approvers,
    int minimumApprovals,
    boolean requireDistinctActors,
    boolean requireReason,
    boolean allowDelegation,
    RejectPolicy rejectPolicy,
    boolean prohibitSelfApproval
) {

    public ApprovalRequirement {
        Objects.requireNonNull(
            approvers, "approvers"
        );
        approvers = Set.copyOf(approvers);
        if (minimumApprovals < 1) {
            throw new IllegalArgumentException(
                "minimumApprovals must be >= 1"
            );
        }
        Objects.requireNonNull(
            rejectPolicy, "rejectPolicy"
        );
    }

    /**
     * 否决策略。
     */
    public enum RejectPolicy {

        /**
         * 任何一票否决。
         */
        ANY_REJECT,

        /**
         * 多数否决。
         */
        MAJORITY_REJECT
    }

    public static ApprovalRequirement
    singleApproval() {
        return new ApprovalRequirement(
            Set.of(),
            1,
            false,
            false,
            false,
            RejectPolicy.ANY_REJECT,
            true
        );
    }

    public static ApprovalRequirement
    quorum(
        int required
    ) {
        return new ApprovalRequirement(
            Set.of(),
            required,
            true,
            false,
            false,
            RejectPolicy.ANY_REJECT,
            true
        );
    }
}
