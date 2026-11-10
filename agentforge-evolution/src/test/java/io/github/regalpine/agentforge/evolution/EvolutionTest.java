package io.github.regalpine.agentforge.evolution;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Evolution tests.
 * AF-ENHANCE-15 §4-10.
 */
class EvolutionTest {

    // ---- CandidateId ----

    @Test
    void candidateIdCreation() {
        var id = new CandidateId(
            "cand-001");
        assertEquals("cand-001",
            id.value());
    }

    @Test
    void candidateIdValidation() {
        assertThrows(
            IllegalArgumentException.class,
            () -> new CandidateId(null));
        assertThrows(
            IllegalArgumentException.class,
            () -> new CandidateId(""));
    }

    // ---- CandidateStatus ----

    @Test
    void candidateStatusChecks() {
        assertTrue(
            CandidateStatus.CREATED
                .isActive());
        assertTrue(
            CandidateStatus.VALIDATING
                .isActive());
        assertFalse(
            CandidateStatus.REJECTED
                .isActive());
        assertTrue(
            CandidateStatus.APPROVED
                .isPassed());
        assertTrue(
            CandidateStatus.RELEASED
                .isPassed());
        assertTrue(
            CandidateStatus.REJECTED
                .isTerminal());
        assertTrue(
            CandidateStatus.EXPIRED
                .isTerminal());
    }

    // ---- Change ----

    @Test
    void changeCreation() {
        var change = Change.replace(
            "agent.prompt.system",
            "old prompt",
            "new prompt",
            "improve compliance");
        assertEquals(
            "agent.prompt.system",
            change.target());
        assertEquals(
            ChangeOperation.REPLACE,
            change.operation());
        assertEquals("old prompt",
            change.before());
        assertEquals("new prompt",
            change.after());
    }

    @Test
    void changeValidation() {
        assertThrows(
            IllegalArgumentException.class,
            () -> new Change(
                null, null, null,
                null, null));
    }

    // ---- CandidateChangeSet ----

    @Test
    void changeSetCreation() {
        var change = Change.replace(
            "prompt", "a", "b",
            "improve");
        var cs = CandidateChangeSet.of(
            List.of(change),
            ChangeScope.PROMPT);
        assertEquals(1, cs.size());
        assertEquals(
            ChangeScope.PROMPT,
            cs.scope());
    }

    @Test
    void changeSetValidation() {
        assertThrows(
            IllegalArgumentException.class,
            () -> CandidateChangeSet.of(
                List.of(), null));
    }

    // ---- EvidenceReference ----

    @Test
    void evidenceCreation() {
        var ev = EvidenceReference.of(
            EvidenceType.EVALUATION_RUN,
            "eval-001");
        assertEquals(
            EvidenceType.EVALUATION_RUN,
            ev.type());
        assertEquals("eval-001",
            ev.id());
    }

    @Test
    void evidenceValidation() {
        assertThrows(
            IllegalArgumentException.class,
            () -> EvidenceReference.of(
                null, null));
    }

    // ---- EvolutionProblem ----

    @Test
    void problemCreation() {
        var problem =
            EvolutionProblem.of(
                "prob-1",
                ProblemCategory.CORRECTNESS,
                ProblemSeverity.HIGH,
                "output failure");
        assertEquals("prob-1",
            problem.problemId());
        assertEquals(
            ProblemCategory.CORRECTNESS,
            problem.category());
        assertEquals(
            ProblemSeverity.HIGH,
            problem.severity());
        assertTrue(
            problem.evidence()
                .isEmpty());
    }

    @Test
    void problemWithEvidence() {
        var problem =
            EvolutionProblem.of(
                "prob-1",
                ProblemCategory.LATENCY,
                ProblemSeverity.MEDIUM,
                "slow response");
        var ev = EvidenceReference.of(
            EvidenceType.TRACE,
            "trace-001");
        problem = problem
            .withEvidence(ev);
        assertEquals(1,
            problem.evidence()
                .size());
    }

    // ---- InMemoryEvolutionCandidate ----

    @Test
    void candidateLifecycle() {
        var id = new CandidateId(
            "cand-1");
        var change = Change.replace(
            "prompt", "a", "b",
            "improve");
        var cs = CandidateChangeSet.of(
            List.of(change),
            ChangeScope.PROMPT);
        var ev = EvidenceReference.of(
            EvidenceType.EVALUATION_RUN,
            "eval-1");

        var cand =
            InMemoryEvolutionCandidate
                .of(id, "agent-a",
                    cs, List.of(ev));
        assertEquals(
            CandidateStatus.CREATED,
            cand.status());
        assertEquals("agent-a",
            cand.baseAgent());
        assertEquals(1,
            cand.evidence().size());

        cand = cand.validate();
        assertEquals(
            CandidateStatus.VALIDATING,
            cand.status());

        cand = cand.experiment();
        assertEquals(
            CandidateStatus.EXPERIMENTING,
            cand.status());

        cand = cand.approve();
        assertEquals(
            CandidateStatus.APPROVED,
            cand.status());
        assertTrue(
            cand.status().isPassed());

        cand = cand.release();
        assertEquals(
            CandidateStatus.RELEASED,
            cand.status());
        assertTrue(
            cand.status().isTerminal());
    }

    @Test
    void candidateRejection() {
        var id = new CandidateId(
            "cand-2");
        var change = Change.replace(
            "model", "a", "b",
            "switch");
        var cs = CandidateChangeSet.of(
            List.of(change),
            ChangeScope.MODEL);

        var cand =
            InMemoryEvolutionCandidate
                .of(id, "agent-b",
                    cs, List.of());
        cand = cand.reject();
        assertEquals(
            CandidateStatus.REJECTED,
            cand.status());
        assertTrue(
            cand.status().isTerminal());
    }

    @Test
    void candidateAddEvidence() {
        var id = new CandidateId(
            "cand-3");
        var change = Change.replace(
            "tool.config", "a", "b",
            "fix");
        var cs = CandidateChangeSet.of(
            List.of(change),
            ChangeScope.TOOL);

        var cand =
            InMemoryEvolutionCandidate
                .of(id, "agent-c",
                    cs, List.of());
        assertEquals(0,
            cand.evidence().size());

        cand = cand.addEvidence(
            EvidenceReference.of(
                EvidenceType.REGRESSION,
                "reg-1"));
        assertEquals(1,
            cand.evidence().size());
    }

    // ---- EvolutionApproval ----

    @Test
    void approvalCreation() {
        var candId = new CandidateId(
            "cand-1");
        var approval =
            EvolutionApproval.of(
                "appr-1", candId,
                "reviewer-alice",
                ApprovalDecision.APPROVED,
                "looks good");
        assertEquals("appr-1",
            approval.approvalId());
        assertEquals("reviewer-alice",
            approval.reviewer());
        assertEquals(
            ApprovalDecision.APPROVED,
            approval.decision());
        assertNotNull(
            approval.timestamp());
    }

    @Test
    void approvalValidation() {
        assertThrows(
            IllegalArgumentException.class,
            () -> EvolutionApproval.of(
                null, null, null,
                null, null));
    }

    // ---- CandidateLineage ----

    @Test
    void rootLineage() {
        var candId = new CandidateId(
            "cand-1");
        var lineage =
            CandidateLineage.root(
                candId, "agent-a");
        assertFalse(
            lineage.hasParent());
        assertEquals("agent-a",
            lineage.baseAgent());
    }

    @Test
    void childLineage() {
        var parentId = new CandidateId(
            "cand-parent");
        var childId = new CandidateId(
            "cand-child");
        var lineage = new CandidateLineage(
            childId, "agent-a",
            parentId, List.of());
        assertTrue(
            lineage.hasParent());
        assertEquals(
            "cand-parent",
            lineage.parentCandidate()
                .value());
    }

    // ---- EvolutionPolicy ----

    @Test
    void policyDefaults() {
        var policy =
            EvolutionPolicy.defaults(
                "default-policy");
        assertEquals("default-policy",
            policy.policyId());
        assertTrue(
            policy.isAllowed(
                ChangeScope.PROMPT));
        assertTrue(
            policy.isAllowed(
                ChangeScope.MODEL));
        assertFalse(
            policy.isAllowed(
                ChangeScope.TOOL));
        assertTrue(
            policy.needsApproval(
                ChangeScope.WORKFLOW));
        assertTrue(
            policy
                .requiredEvaluation());
        assertTrue(
            policy
                .requiredRegressionCheck());
        assertFalse(
            policy
                .autoReleaseEnabled());
    }

    // ---- ValidationResult ----

    @Test
    void validationResultPass() {
        var result =
            ValidationResult.pass(
                "test-gate");
        assertTrue(result.passed());
        assertEquals("test-gate",
            result.gateName());
    }

    @Test
    void validationResultFail() {
        var result =
            ValidationResult.fail(
                "security-gate",
                "violation found");
        assertFalse(result.passed());
        assertEquals("violation_found",
            result.message()
                .replace(" ", "_"));
    }

    // ---- DefaultEvolutionLoop ----

    @Test
    void evolutionLoopSuccess() {
        var generator =
            new TestGenerator();
        var loop =
            new DefaultEvolutionLoop(
                generator);

        var problem =
            EvolutionProblem.of(
                "prob-1",
                ProblemCategory.CORRECTNESS,
                ProblemSeverity.HIGH,
                "test problem");
        var ctx = EvolutionContext.of(
            "agent-a", "1.0.0");
        var gates = List.of(
            passGate("test"),
            passGate("eval"),
            passGate("security"));

        var result = loop.execute(
            problem, ctx, gates);
        assertEquals(
            CandidateStatus.APPROVED,
            result.status());
    }

    @Test
    void evolutionLoopGateFail() {
        var generator =
            new TestGenerator();
        var loop =
            new DefaultEvolutionLoop(
                generator);

        var problem =
            EvolutionProblem.of(
                "prob-2",
                ProblemCategory.LATENCY,
                ProblemSeverity.MEDIUM,
                "test problem");
        var ctx = EvolutionContext.of(
            "agent-a", "1.0.0");
        var gates = List.of(
            passGate("test"),
            failGate("security"),
            passGate("eval"));

        var result = loop.execute(
            problem, ctx, gates);
        assertEquals(
            CandidateStatus.REJECTED,
            result.status());
    }

    // ---- ChangeScope / enums ----

    @Test
    void changeScopeValues() {
        assertEquals(7,
            ChangeScope.values().length);
    }

    @Test
    void evidenceTypeValues() {
        assertEquals(8,
            EvidenceType.values()
                .length);
    }

    @Test
    void problemCategoryValues() {
        assertEquals(8,
            ProblemCategory.values()
                .length);
    }

    @Test
    void problemSeverityValues() {
        assertEquals(5,
            ProblemSeverity.values()
                .length);
    }

    @Test
    void approvalDecisionValues() {
        assertEquals(3,
            ApprovalDecision.values()
                .length);
    }

    // ---- helpers ----

    private ValidationGate passGate(
        String name
    ) {
        return new ValidationGate() {
            @Override
            public String name() {
                return name;
            }

            @Override
            public ValidationResult validate(
                EvolutionCandidate c
            ) {
                return ValidationResult
                    .pass(name);
            }
        };
    }

    private ValidationGate failGate(
        String name
    ) {
        return new ValidationGate() {
            @Override
            public String name() {
                return name;
            }

            @Override
            public ValidationResult validate(
                EvolutionCandidate c
            ) {
                return ValidationResult
                    .fail(name,
                        "failed");
            }
        };
    }

    private static class TestGenerator
        implements CandidateGenerator {

        @Override
        public List<EvolutionCandidate>
            generate(
                EvolutionProblem problem,
                EvolutionContext ctx
            ) {
            var change = Change.replace(
                "prompt", "a", "b",
                "fix");
            var cs =
                CandidateChangeSet.of(
                    List.of(change),
                    ChangeScope.PROMPT);
            var cand =
                InMemoryEvolutionCandidate
                    .of(
                        new CandidateId(
                            "gen-1"),
                        ctx.agentReference(),
                        cs,
                        List.of());
            return List.of(cand);
        }
    }
}
