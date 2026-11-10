package io.github.regalpine.agentforge.runtime.hitl;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;

/**
 * 审批审计记录。
 *
 * <p>不可篡改。
 * 对应 AF-ENHANCE-03 §35。</p>
 */
public record ApprovalAudit(
    String auditId,
    String approvalId,
    String requestId,
    String executionId,
    String actorId,
    ApprovalPrincipalType actorType,
    ApprovalDecisionType decision,
    String reason,
    ApprovalStatus beforeState,
    ApprovalStatus afterState,
    Instant timestamp,
    String source,
    Map<String, Object>
        authenticationContext,
    Map<String, Object>
        authorizationContext
) {

    public ApprovalAudit {
        Objects.requireNonNull(
            auditId, "auditId"
        );
        Objects.requireNonNull(
            requestId, "requestId"
        );
        Objects.requireNonNull(
            executionId, "executionId"
        );
        Objects.requireNonNull(
            actorId, "actorId"
        );
        Objects.requireNonNull(
            decision, "decision"
        );
        Objects.requireNonNull(
            timestamp, "timestamp"
        );
        authenticationContext =
            authenticationContext == null
                ? Map.of()
                : Map.copyOf(
                    authenticationContext
                  );
        authorizationContext =
            authorizationContext == null
                ? Map.of()
                : Map.copyOf(
                    authorizationContext
                  );
    }
}
