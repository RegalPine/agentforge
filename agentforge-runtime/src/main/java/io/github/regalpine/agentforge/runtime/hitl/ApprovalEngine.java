package io.github.regalpine.agentforge.runtime.hitl;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 审批引擎。
 *
 * <p>核心 HITL 运行时，负责：
 * <ul>
 *   <li>Policy 评估</li>
 *   <li>ApprovalRequest 创建</li>
 *   <li>Decision 处理（单人/多人/否决）</li>
 *   <li>防自审批</li>
 *   <li>幂等性</li>
 *   <li>超时处理</li>
 *   <li>审计日志</li>
 * </ul>
 * 对应 AF-ENHANCE-03。</p>
 */
public final class ApprovalEngine {

    private final Map<String,
        ApprovalPolicy> policies =
        new ConcurrentHashMap<>();

    private final Map<String,
        ApprovalRequest> requests =
        new ConcurrentHashMap<>();

    private final List<ApprovalAudit>
        auditLog =
        new ArrayList<>();

    private final Set<String>
        processedDecisions =
        new HashSet<>();

    private final List<ApprovalListener>
        listeners =
        new ArrayList<>();

    // --- Policy Management ---

    public void registerPolicy(
        ApprovalPolicy policy
    ) {
        Objects.requireNonNull(
            policy, "policy"
        );
        policies.put(
            policy.id(), policy
        );
    }

    public ApprovalPolicy getPolicy(
        String policyId
    ) {
        return policies.get(policyId);
    }

    // --- Policy Evaluation ---

    /**
     * 评估动作是否需要审批。
     */
    public PolicyEvaluationResult
    evaluate(
        String action,
        RiskAssessment risk
    ) {
        Objects.requireNonNull(
            action, "action"
        );
        Objects.requireNonNull(
            risk, "risk"
        );

        ApprovalPolicy policy =
            findMatchingPolicy(
                action, risk
            );

        if (policy == null) {
            return PolicyEvaluationResult
                .ALLOW;
        }

        if (!policy.enabled()) {
            return PolicyEvaluationResult
                .ALLOW;
        }

        if (risk.level().isAtLeast(
            policy.minRiskLevel()
        )) {
            return PolicyEvaluationResult
                .APPROVAL_REQUIRED;
        }

        return PolicyEvaluationResult
            .ALLOW;
    }

    /**
     * 评估并创建审批请求。
     */
    public ApprovalRequest
    createApprovalRequest(
        String executionId,
        String traceId,
        String agentId,
        String action,
        RiskAssessment risk,
        ApprovalContext context
    ) {
        ApprovalPolicy policy =
            findMatchingPolicy(
                action, risk
            );

        if (policy == null) {
            throw new IllegalStateException(
                "No matching policy for: "
                    + action
            );
        }

        Instant expiresAt = null;
        if (policy.timeout() != null) {
            expiresAt = Instant.now()
                .plus(policy.timeout());
        }

        ApprovalRequest request =
            ApprovalRequest.builder()
                .executionId(executionId)
                .traceId(traceId)
                .agentId(agentId)
                .policyId(policy.id())
                .risk(risk)
                .context(context)
                .requirement(
                    policy.requirement()
                )
                .createdAt(Instant.now())
                .expiresAt(expiresAt)
                .build();

        requests.put(
            request.id(), request
        );

        notifyListeners(
            "approval.created",
            request
        );

        return request;
    }

    // --- Decision Processing ---

    /**
     * 提交审批决策。
     *
     * <p>支持幂等性（§51）。</p>
     */
    public synchronized
    DecisionResult submitDecision(
        String requestId,
        ApprovalDecision decision
    ) {
        ApprovalRequest request =
            requests.get(requestId);

        if (request == null) {
            throw new IllegalArgumentException(
                "Unknown request: "
                    + requestId
            );
        }

        // 幂等性检查
        String decisionKey =
            decision.id() + ":"
                + decision.actor().actorId();
        if (processedDecisions
            .contains(decisionKey)
        ) {
            return DecisionResult
                .idempotent(
                    request.status()
                );
        }

        // 防自审批（§38）
        if (request.requirement()
            .prohibitSelfApproval()
        ) {
            if (decision.actor().actorId()
                .equals(request.agentId())
            ) {
                throw new
                    SelfApprovalException(
                    "Agent cannot approve "
                        + "own request"
                );
            }
        }

        // 超时检查
        if (request.isExpired()) {
            ApprovalStatus before =
                request.status();
            request.transitionTo(
                ApprovalStatus.EXPIRED,
                request.version()
            );
            recordAudit(
                request, decision,
                before,
                ApprovalStatus.EXPIRED
            );
            return DecisionResult
                .expired();
        }

        ApprovalStatus before =
            request.status();

        request.addDecision(decision);

        // 一票否决（§19）
        if (decision.type()
            == ApprovalDecisionType
                .REJECT
        ) {
            if (request.requirement()
                .rejectPolicy()
                == ApprovalRequirement
                    .RejectPolicy
                    .ANY_REJECT
            ) {
                request.transitionTo(
                    ApprovalStatus
                        .REJECTED,
                    request.version()
                );
                recordAudit(
                    request, decision,
                    before,
                    ApprovalStatus
                        .REJECTED
                );
                notifyListeners(
                    "approval.rejected",
                    request
                );
                return DecisionResult
                    .rejected();
            }
        }

        // ABORT
        if (decision.type()
            == ApprovalDecisionType
                .ABORT
        ) {
            request.transitionTo(
                ApprovalStatus.ABORTED,
                request.version()
            );
            recordAudit(
                request, decision,
                before,
                ApprovalStatus.ABORTED
            );
            notifyListeners(
                "approval.aborted",
                request
            );
            return DecisionResult
                .aborted();
        }

        // MODIFY
        if (decision.type()
            == ApprovalDecisionType
                .MODIFY
        ) {
            request.transitionTo(
                ApprovalStatus.MODIFIED,
                request.version()
            );
            recordAudit(
                request, decision,
                before,
                ApprovalStatus.MODIFIED
            );
            notifyListeners(
                "approval.modified",
                request
            );
            return DecisionResult
                .modified(
                    decision
                        .modifications()
                );
        }

        // APPROVE - 检查 Quorum
        if (decision.type()
            == ApprovalDecisionType
                .APPROVE
        ) {
            long approvalCount =
                request.decisions()
                    .stream()
                    .filter(d ->
                        d.type()
                        == ApprovalDecisionType
                            .APPROVE
                    )
                    .count();

            int required =
                request.requirement()
                    .minimumApprovals();

            if (approvalCount
                >= required
            ) {
                request.transitionTo(
                    ApprovalStatus
                        .APPROVED,
                    request.version()
                );
                recordAudit(
                    request, decision,
                    before,
                    ApprovalStatus
                        .APPROVED
                );
                notifyListeners(
                    "approval.approved",
                    request
                );
                return DecisionResult
                    .approved();
            }

            // 尚未满足 Quorum
            processedDecisions.add(
                decisionKey
            );
            return DecisionResult
                .pending(
                    required
                        - approvalCount
                );
        }

        processedDecisions.add(
            decisionKey
        );
        return DecisionResult
            .pending(0);
    }

    // --- Timeout ---

    /**
     * 处理超时。
     */
    public void handleTimeout(
        String requestId
    ) {
        ApprovalRequest request =
            requests.get(requestId);

        if (request == null
            || request.status()
                .isTerminal()
        ) {
            return;
        }

        if (!request.isExpired()) {
            return;
        }

        ApprovalPolicy policy =
            policies.get(
                request.policyId()
            );

        TimeoutAction action =
            policy != null
                ? policy.timeoutAction()
                : TimeoutAction.REJECT;

        ApprovalStatus before =
            request.status();

        switch (action) {
            case REJECT -> {
                request.transitionTo(
                    ApprovalStatus
                        .EXPIRED,
                    request.version()
                );
                recordAuditTimeout(
                    request, before,
                    ApprovalStatus.EXPIRED
                );
            }
            case ABORT -> {
                request.transitionTo(
                    ApprovalStatus
                        .ABORTED,
                    request.version()
                );
                recordAuditTimeout(
                    request, before,
                    ApprovalStatus.ABORTED
                );
            }
            case ESCALATE -> {
                request.transitionTo(
                    ApprovalStatus
                        .ESCALATED,
                    request.version()
                );
                recordAuditTimeout(
                    request, before,
                    ApprovalStatus
                        .ESCALATED
                );
            }
            case REMAIN_PAUSED -> {
                // 保持暂停，不做操作
            }
        }
    }

    // --- Query ---

    public ApprovalRequest getRequest(
        String requestId
    ) {
        return requests.get(requestId);
    }

    public List<ApprovalRequest>
    getPendingRequests() {
        return requests.values()
            .stream()
            .filter(r ->
                r.status()
                == ApprovalStatus
                    .PENDING
            )
            .toList();
    }

    public List<ApprovalAudit>
    getAuditLog() {
        return List.copyOf(auditLog);
    }

    // --- Listeners ---

    public void addListener(
        ApprovalListener listener
    ) {
        listeners.add(listener);
    }

    // --- Internal ---

    private ApprovalPolicy
    findMatchingPolicy(
        String action,
        RiskAssessment risk
    ) {
        return policies.values()
            .stream()
            .filter(p ->
                p.enabled()
                && p.trigger().equals(
                    action
                )
            )
            .min((a, b) ->
                Integer.compare(
                    b.priority(),
                    a.priority()
                )
            )
            .orElse(null);
    }

    private void recordAudit(
        ApprovalRequest request,
        ApprovalDecision decision,
        ApprovalStatus before,
        ApprovalStatus after
    ) {
        ApprovalAudit audit =
            new ApprovalAudit(
                "audit-"
                    + UUID.randomUUID(),
                request.id(),
                request.id(),
                request.executionId(),
                decision.actor()
                    .actorId(),
                decision.actor()
                    .actorType(),
                decision.type(),
                decision.reason(),
                before, after,
                Instant.now(),
                "ApprovalEngine",
                Map.of(),
                Map.of()
            );
        auditLog.add(audit);
    }

    private void recordAuditTimeout(
        ApprovalRequest request,
        ApprovalStatus before,
        ApprovalStatus after
    ) {
        ApprovalAudit audit =
            new ApprovalAudit(
                "audit-"
                    + UUID.randomUUID(),
                request.id(),
                request.id(),
                request.executionId(),
                "system",
                ApprovalPrincipalType
                    .SERVICE,
                ApprovalDecisionType
                    .ABORT,
                "Timeout",
                before, after,
                Instant.now(),
                "ApprovalEngine"
                    + ".timeout",
                Map.of(),
                Map.of()
            );
        auditLog.add(audit);
    }

    private void notifyListeners(
        String eventType,
        ApprovalRequest request
    ) {
        for (var listener
            : listeners
        ) {
            listener.onEvent(
                eventType, request
            );
        }
    }

    // --- Result Types ---

    public record DecisionResult(
        ResultType type,
        ApprovalStatus status,
        long remainingApprovals,
        Map<String, Object> modifications
    ) {

        public enum ResultType {
            APPROVED,
            REJECTED,
            MODIFIED,
            ABORTED,
            PENDING,
            EXPIRED,
            IDEMPOTENT
        }

        static DecisionResult
        approved() {
            return new DecisionResult(
                ResultType.APPROVED,
                ApprovalStatus.APPROVED,
                0, Map.of()
            );
        }

        static DecisionResult
        rejected() {
            return new DecisionResult(
                ResultType.REJECTED,
                ApprovalStatus.REJECTED,
                0, Map.of()
            );
        }

        static DecisionResult
        modified(
            Map<String, Object> mods
        ) {
            return new DecisionResult(
                ResultType.MODIFIED,
                ApprovalStatus.MODIFIED,
                0, mods
            );
        }

        static DecisionResult
        aborted() {
            return new DecisionResult(
                ResultType.ABORTED,
                ApprovalStatus.ABORTED,
                0, Map.of()
            );
        }

        static DecisionResult
        pending(long remaining) {
            return new DecisionResult(
                ResultType.PENDING,
                ApprovalStatus.PENDING,
                remaining, Map.of()
            );
        }

        static DecisionResult
        expired() {
            return new DecisionResult(
                ResultType.EXPIRED,
                ApprovalStatus.EXPIRED,
                0, Map.of()
            );
        }

        static DecisionResult
        idempotent(
            ApprovalStatus current
        ) {
            return new DecisionResult(
                ResultType.IDEMPOTENT,
                current,
                0, Map.of()
            );
        }
    }

    /**
     * 审批事件监听器。
     */
    public interface
    ApprovalListener {
        void onEvent(
            String eventType,
            ApprovalRequest request
        );
    }

    /**
     * 自审批异常。
     */
    public static class
    SelfApprovalException
    extends RuntimeException {
        public SelfApprovalException(
            String message
        ) {
            super(message);
        }
    }
}
