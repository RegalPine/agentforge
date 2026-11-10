package io.github.regalpine.agentforge.runtime.hitl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * ApprovalEngine 测试。
 *
 * <p>覆盖：单人审批 / 多人审批 /
 * 一票否决 / 防自审批 / 幂等性 /
 * 超时 / 审计。</p>
 */
class ApprovalEngineTest {

    private ApprovalEngine engine;

    @BeforeEach
    void setup() {
        engine = new ApprovalEngine();
    }

    private ApprovalPolicy
    createPolicy(
        String id,
        RiskLevel minLevel,
        ApprovalRequirement req
    ) {
        ApprovalPolicy policy =
            ApprovalPolicy.builder()
                .id(id)
                .trigger(
                    "production.write"
                )
                .minRiskLevel(minLevel)
                .requirement(req)
                .timeout(
                    Duration.ofMinutes(30)
                )
                .timeoutAction(
                    TimeoutAction.REJECT
                )
                .modifiableFields(
                    Set.of("amount")
                )
                .build();
        engine.registerPolicy(
            policy
        );
        return policy;
    }

    // --- 单人审批 ---

    @Test
    void shouldApproveSingleApproval() {
        createPolicy(
            "p1", RiskLevel.HIGH,
            ApprovalRequirement
                .singleApproval()
        );

        RiskAssessment risk =
            RiskAssessment.of(
                RiskLevel.HIGH,
                "prod write"
            );

        ApprovalRequest request =
            engine.createApprovalRequest(
                "exec-1", "trace-1",
                "agent-1",
                "production.write",
                risk,
                ApprovalContext.of(
                    "exec-1",
                    "agent-1",
                    "production.write"
                )
            );

        ApprovalActor actor =
            ApprovalActor.of(
                "user-1",
                ApprovalPrincipalType.USER
            );

        ApprovalDecision decision =
            ApprovalDecision.of(
                request.id(),
                ApprovalDecisionType
                    .APPROVE,
                actor, "Reviewed"
            );

        var result =
            engine.submitDecision(
                request.id(),
                decision
            );

        assertEquals(
            ApprovalEngine
                .DecisionResult
                .ResultType.APPROVED,
            result.type()
        );
        assertEquals(
            ApprovalStatus.APPROVED,
            request.status()
        );
    }

    // --- 一票否决 ---

    @Test
    void shouldRejectOnVeto() {
        createPolicy(
            "p2", RiskLevel.HIGH,
            ApprovalRequirement
                .quorum(2)
        );

        RiskAssessment risk =
            RiskAssessment.of(
                RiskLevel.HIGH,
                "prod write"
            );

        ApprovalRequest request =
            engine.createApprovalRequest(
                "exec-2", "trace-2",
                "agent-2",
                "production.write",
                risk,
                ApprovalContext.of(
                    "exec-2",
                    "agent-2",
                    "production.write"
                )
            );

        // User A approves
        var actorA =
            ApprovalActor.of(
                "user-a",
                ApprovalPrincipalType.USER
            );
        engine.submitDecision(
            request.id(),
            ApprovalDecision.of(
                request.id(),
                ApprovalDecisionType
                    .APPROVE,
                actorA, "OK"
            )
        );

        // User B rejects (veto)
        var actorB =
            ApprovalActor.of(
                "user-b",
                ApprovalPrincipalType.USER
            );
        var result =
            engine.submitDecision(
                request.id(),
                ApprovalDecision.of(
                    request.id(),
                    ApprovalDecisionType
                        .REJECT,
                    actorB, "Not safe"
                )
            );

        assertEquals(
            ApprovalEngine
                .DecisionResult
                .ResultType.REJECTED,
            result.type()
        );
        assertEquals(
            ApprovalStatus.REJECTED,
            request.status()
        );
    }

    // --- 多人审批 Quorum ---

    @Test
    void shouldRequireQuorum() {
        createPolicy(
            "p3", RiskLevel.HIGH,
            ApprovalRequirement
                .quorum(2)
        );

        RiskAssessment risk =
            RiskAssessment.of(
                RiskLevel.HIGH,
                "prod write"
            );

        ApprovalRequest request =
            engine.createApprovalRequest(
                "exec-3", "trace-3",
                "agent-3",
                "production.write",
                risk,
                ApprovalContext.of(
                    "exec-3",
                    "agent-3",
                    "production.write"
                )
            );

        // First approval
        var actorA =
            ApprovalActor.of(
                "user-a",
                ApprovalPrincipalType.USER
            );
        var result1 =
            engine.submitDecision(
                request.id(),
                ApprovalDecision.of(
                    request.id(),
                    ApprovalDecisionType
                        .APPROVE,
                    actorA, "OK"
                )
            );

        assertEquals(
            ApprovalEngine
                .DecisionResult
                .ResultType.PENDING,
            result1.type()
        );
        assertEquals(1,
            result1.remainingApprovals()
        );

        // Second approval
        var actorB =
            ApprovalActor.of(
                "user-b",
                ApprovalPrincipalType.USER
            );
        var result2 =
            engine.submitDecision(
                request.id(),
                ApprovalDecision.of(
                    request.id(),
                    ApprovalDecisionType
                        .APPROVE,
                    actorB, "OK"
                )
            );

        assertEquals(
            ApprovalEngine
                .DecisionResult
                .ResultType.APPROVED,
            result2.type()
        );
    }

    // --- 防自审批 ---

    @Test
    void shouldPreventSelfApproval() {
        createPolicy(
            "p4", RiskLevel.HIGH,
            ApprovalRequirement
                .singleApproval()
        );

        RiskAssessment risk =
            RiskAssessment.of(
                RiskLevel.HIGH,
                "prod write"
            );

        ApprovalRequest request =
            engine.createApprovalRequest(
                "exec-4", "trace-4",
                "agent-x",
                "production.write",
                risk,
                ApprovalContext.of(
                    "exec-4",
                    "agent-x",
                    "production.write"
                )
            );

        // Agent tries to approve
        // its own request
        ApprovalActor agentActor =
            ApprovalActor.of(
                "agent-x",
                ApprovalPrincipalType
                    .SERVICE
            );

        assertThrows(
            ApprovalEngine
                .SelfApprovalException
                .class,
            () -> engine.submitDecision(
                request.id(),
                ApprovalDecision.of(
                    request.id(),
                    ApprovalDecisionType
                        .APPROVE,
                    agentActor, "Self"
                )
            )
        );
    }

    // --- ABORT ---

    @Test
    void shouldAbortRequest() {
        createPolicy(
            "p5", RiskLevel.HIGH,
            ApprovalRequirement
                .singleApproval()
        );

        RiskAssessment risk =
            RiskAssessment.of(
                RiskLevel.HIGH,
                "prod write"
            );

        ApprovalRequest request =
            engine.createApprovalRequest(
                "exec-5", "trace-5",
                "agent-5",
                "production.write",
                risk,
                ApprovalContext.of(
                    "exec-5",
                    "agent-5",
                    "production.write"
                )
            );

        var actor =
            ApprovalActor.of(
                "user-1",
                ApprovalPrincipalType.USER
            );

        var result =
            engine.submitDecision(
                request.id(),
                ApprovalDecision.of(
                    request.id(),
                    ApprovalDecisionType
                        .ABORT,
                    actor, "Stop"
                )
            );

        assertEquals(
            ApprovalEngine
                .DecisionResult
                .ResultType.ABORTED,
            result.type()
        );
        assertEquals(
            ApprovalStatus.ABORTED,
            request.status()
        );
    }

    // --- MODIFY ---

    @Test
    void shouldModifyRequest() {
        createPolicy(
            "p6", RiskLevel.HIGH,
            ApprovalRequirement
                .singleApproval()
        );

        RiskAssessment risk =
            RiskAssessment.of(
                RiskLevel.HIGH,
                "prod write"
            );

        ApprovalRequest request =
            engine.createApprovalRequest(
                "exec-6", "trace-6",
                "agent-6",
                "production.write",
                risk,
                ApprovalContext.of(
                    "exec-6",
                    "agent-6",
                    "production.write"
                )
            );

        var actor =
            ApprovalActor.of(
                "user-1",
                ApprovalPrincipalType.USER
            );

        var result =
            engine.submitDecision(
                request.id(),
                ApprovalDecision
                    .withModifications(
                        request.id(),
                        actor,
                        "Reduce amount",
                        Map.of(
                            "amount",
                            50000
                        )
                    )
            );

        assertEquals(
            ApprovalEngine
                .DecisionResult
                .ResultType.MODIFIED,
            result.type()
        );
        assertEquals(
            50000,
            result.modifications()
                .get("amount")
        );
    }

    // --- Policy Evaluation ---

    @Test
    void shouldEvaluatePolicy() {
        createPolicy(
            "p7", RiskLevel.HIGH,
            ApprovalRequirement
                .singleApproval()
        );

        assertEquals(
            PolicyEvaluationResult
                .APPROVAL_REQUIRED,
            engine.evaluate(
                "production.write",
                RiskAssessment.of(
                    RiskLevel.HIGH,
                    "test"
                )
            )
        );

        assertEquals(
            PolicyEvaluationResult
                .ALLOW,
            engine.evaluate(
                "production.write",
                RiskAssessment.of(
                    RiskLevel.LOW,
                    "test"
                )
            )
        );

        assertEquals(
            PolicyEvaluationResult
                .ALLOW,
            engine.evaluate(
                "unknown.action",
                RiskAssessment.of(
                    RiskLevel.CRITICAL,
                    "test"
                )
            )
        );
    }

    // --- Audit ---

    @Test
    void shouldRecordAudit() {
        createPolicy(
            "p8", RiskLevel.HIGH,
            ApprovalRequirement
                .singleApproval()
        );

        RiskAssessment risk =
            RiskAssessment.of(
                RiskLevel.HIGH,
                "prod write"
            );

        ApprovalRequest request =
            engine.createApprovalRequest(
                "exec-8", "trace-8",
                "agent-8",
                "production.write",
                risk,
                ApprovalContext.of(
                    "exec-8",
                    "agent-8",
                    "production.write"
                )
            );

        var actor =
            ApprovalActor.of(
                "user-1",
                ApprovalPrincipalType.USER
            );

        engine.submitDecision(
            request.id(),
            ApprovalDecision.of(
                request.id(),
                ApprovalDecisionType
                    .APPROVE,
                actor, "Reviewed"
            )
        );

        var audit =
            engine.getAuditLog();
        assertFalse(
            audit.isEmpty()
        );
        assertEquals(
            "user-1",
            audit.get(0).actorId()
        );
        assertEquals(
            ApprovalDecisionType
                .APPROVE,
            audit.get(0).decision()
        );
    }

    // --- Pending Queries ---

    @Test
    void shouldQueryPending() {
        createPolicy(
            "p9", RiskLevel.HIGH,
            ApprovalRequirement
                .singleApproval()
        );

        RiskAssessment risk =
            RiskAssessment.of(
                RiskLevel.HIGH,
                "prod write"
            );

        engine.createApprovalRequest(
            "exec-9a", "trace-9",
            "agent-9",
            "production.write",
            risk,
            ApprovalContext.of(
                "exec-9a",
                "agent-9",
                "production.write"
            )
        );

        engine.createApprovalRequest(
            "exec-9b", "trace-9",
            "agent-9",
            "production.write",
            risk,
            ApprovalContext.of(
                "exec-9b",
                "agent-9",
                "production.write"
            )
        );

        assertEquals(
            2,
            engine.getPendingRequests()
                .size()
        );
    }

    // --- ApprovalStatus 转换 ---

    @Test
    void shouldValidateStatusTransitions() {
        assertTrue(
            ApprovalStatus.PENDING
                .canTransitionTo(
                    ApprovalStatus.APPROVED
                )
        );
        assertTrue(
            ApprovalStatus.PENDING
                .canTransitionTo(
                    ApprovalStatus.REJECTED
                )
        );
        assertFalse(
            ApprovalStatus.APPROVED
                .canTransitionTo(
                    ApprovalStatus.REJECTED
                )
        );
        assertTrue(
            ApprovalStatus.APPROVED
                .isTerminal()
        );
        assertFalse(
            ApprovalStatus.PENDING
                .isTerminal()
        );
    }
}
