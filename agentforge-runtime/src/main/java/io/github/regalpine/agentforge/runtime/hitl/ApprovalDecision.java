package io.github.regalpine.agentforge.runtime.hitl;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * 审批决策。
 *
 * <p>不可变。对应 AF-ENHANCE-03 §20。
 * Decision 一旦创建不可修改（§45）。</p>
 */
public record ApprovalDecision(
    String id,
    String requestId,
    ApprovalDecisionType type,
    ApprovalActor actor,
    String reason,
    Map<String, Object> modifications,
    Instant decidedAt
) {

    public ApprovalDecision {
        Objects.requireNonNull(
            id, "id"
        );
        Objects.requireNonNull(
            requestId, "requestId"
        );
        Objects.requireNonNull(
            type, "type"
        );
        Objects.requireNonNull(
            actor, "actor"
        );
        Objects.requireNonNull(
            decidedAt, "decidedAt"
        );
        if (modifications != null) {
            modifications =
                Map.copyOf(modifications);
        } else {
            modifications = Map.of();
        }
    }

    public static ApprovalDecision of(
        String requestId,
        ApprovalDecisionType type,
        ApprovalActor actor,
        String reason
    ) {
        return new ApprovalDecision(
            "dec-"
                + UUID.randomUUID(),
            requestId, type, actor,
            reason, Map.of(),
            Instant.now()
        );
    }

    public static ApprovalDecision
    withModifications(
        String requestId,
        ApprovalActor actor,
        String reason,
        Map<String, Object> mods
    ) {
        return new ApprovalDecision(
            "dec-"
                + UUID.randomUUID(),
            requestId,
            ApprovalDecisionType.MODIFY,
            actor, reason, mods,
            Instant.now()
        );
    }
}
