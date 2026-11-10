package io.github.regalpine.agentforge.runtime.hitl;

import java.time.Instant;
import java.util.Objects;

/**
 * 执行暂停。
 *
 * <p>对应 AF-ENHANCE-03 §30。</p>
 */
public record ExecutionPause(
    String executionId,
    PauseReason reason,
    String relatedObjectId,
    Instant pausedAt,
    String checkpointId
) {

    public ExecutionPause {
        Objects.requireNonNull(
            executionId, "executionId"
        );
        Objects.requireNonNull(
            reason, "reason"
        );
        Objects.requireNonNull(
            pausedAt, "pausedAt"
        );
    }

    public static ExecutionPause
    forApproval(
        String executionId,
        String approvalRequestId,
        String checkpointId
    ) {
        return new ExecutionPause(
            executionId,
            PauseReason.HUMAN_APPROVAL,
            approvalRequestId,
            Instant.now(),
            checkpointId
        );
    }
}
