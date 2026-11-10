package io.github.regalpine.agentforge.evolution;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Deep boundary / immutability /
 * edge-case tests for evolution.
 * Complements EvolutionTest.
 */
class EvolutionBoundaryTest {

    // ===== CandidateId =====

    @Test
    void candidateId_rejectsBlank() {
        assertThrows(
            IllegalArgumentException.class,
            () -> new CandidateId(" ")
        );
    }

    @Test
    void candidateId_equality() {
        var a = new CandidateId("c1");
        var b = new CandidateId("c1");
        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
    }

    // ===== CandidateStatus exhaustive =====

    @Test
    void candidateStatus_allIsActive() {
        assertTrue(CandidateStatus.CREATED.isActive());
        assertTrue(CandidateStatus.ANALYZING.isActive());
        assertTrue(CandidateStatus.GENERATED.isActive());
        assertTrue(CandidateStatus.VALIDATING.isActive());
        assertTrue(CandidateStatus.EXPERIMENTING.isActive());
        assertTrue(CandidateStatus.REVIEW_REQUIRED.isActive());
        assertTrue(CandidateStatus.APPROVED.isActive());
        assertFalse(CandidateStatus.REJECTED.isActive());
        assertFalse(CandidateStatus.RELEASED.isActive());
        assertFalse(CandidateStatus.EXPIRED.isActive());
    }

    @Test
    void candidateStatus_allIsPassed() {
        assertFalse(CandidateStatus.CREATED.isPassed());
        assertFalse(CandidateStatus.ANALYZING.isPassed());
        assertFalse(CandidateStatus.GENERATED.isPassed());
        assertFalse(CandidateStatus.VALIDATING.isPassed());
        assertFalse(CandidateStatus.EXPERIMENTING.isPassed());
        assertFalse(CandidateStatus.REVIEW_REQUIRED.isPassed());
        assertTrue(CandidateStatus.APPROVED.isPassed());
        assertFalse(CandidateStatus.REJECTED.isPassed());
        assertTrue(CandidateStatus.RELEASED.isPassed());
        assertFalse(CandidateStatus.EXPIRED.isPassed());
    }

    @Test
    void candidateStatus_allIsTerminal() {
        assertFalse(CandidateStatus.CREATED.isTerminal());
        assertFalse(CandidateStatus.ANALYZING.isTerminal());
        assertFalse(CandidateStatus.GENERATED.isTerminal());
        assertFalse(CandidateStatus.VALIDATING.isTerminal());
        assertFalse(CandidateStatus.EXPERIMENTING.isTerminal());
        assertFalse(CandidateStatus.REVIEW_REQUIRED.isTerminal());
        assertFalse(CandidateStatus.APPROVED.isTerminal());
        assertTrue(CandidateStatus.REJECTED.isTerminal());
        assertTrue(CandidateStatus.RELEASED.isTerminal());
        assertTrue(CandidateStatus.EXPIRED.isTerminal());
    }

    // ===== Change =====

    @Test
    void change_rejectsBlankTarget() {
        assertThrows(
            IllegalArgumentException.class,
            () -> new Change(
                " ", ChangeOperation.REPLACE,
                null, null, null
            )
        );
    }

    @Test
    void change_rejectsNullOperation() {
        assertThrows(
            IllegalArgumentException.class,
            () -> new Change(
                "target", null,
                null, null, null
            )
        );
    }

    @Test
    void change_nullBeforeAfterAllowed() {
        var change = new Change(
            "target",
            ChangeOperation.ADD,
            null, "new", "reason"
        );

        assertNull(change.before());
        assertEquals("new", change.after());
    }

    @Test
    void change_equality() {
        var a = Change.replace(
            "t", "b", "a", "r"
        );
        var b = Change.replace(
            "t", "b", "a", "r"
        );
        assertEquals(a, b);
    }

    // ===== CandidateChangeSet =====

    @Test
    void changeSet_rejectsEmptyChanges() {
        assertThrows(
            IllegalArgumentException.class,
            () -> CandidateChangeSet.of(
                List.of(), ChangeScope.PROMPT
            )
        );
    }

    @Test
    void changeSet_rejectsNullScope() {
        var change = Change.replace(
            "t", "a", "b", "r"
        );
        assertThrows(
            IllegalArgumentException.class,
            () -> CandidateChangeSet.of(
                List.of(change), null
            )
        );
    }

    @Test
    void changeSet_defensiveCopy() {
        var change = Change.replace(
            "t", "a", "b", "r"
        );
        var list = new ArrayList<>(
            List.of(change)
        );

        var cs = CandidateChangeSet.of(
            list, ChangeScope.PROMPT
        );

        list.add(
            Change.replace("t2", "x", "y", "z")
        );
        assertEquals(1, cs.size());
    }

    @Test
    void changeSet_unmodifiableChanges() {
        var change = Change.replace(
            "t", "a", "b", "r"
        );
        var cs = CandidateChangeSet.of(
            List.of(change), ChangeScope.PROMPT
        );

        assertThrows(
            UnsupportedOperationException.class,
            () -> cs.changes().add(null)
        );
    }

    // ===== EvidenceReference =====

    @Test
    void evidence_rejectsNullType() {
        assertThrows(
            IllegalArgumentException.class,
            () -> EvidenceReference.of(null, "id")
        );
    }

    @Test
    void evidence_rejectsBlankId() {
        assertThrows(
            IllegalArgumentException.class,
            () -> EvidenceReference.of(
                EvidenceType.TRACE, " "
            )
        );
    }

    @Test
    void evidence_equality() {
        var a = EvidenceReference.of(
            EvidenceType.TRACE, "t1"
        );
        var b = EvidenceReference.of(
            EvidenceType.TRACE, "t1"
        );
        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
    }

    // ===== EvolutionProblem =====

    @Test
    void problem_rejectsBlankId() {
        assertThrows(
            IllegalArgumentException.class,
            () -> EvolutionProblem.of(
                " ", ProblemCategory.CORRECTNESS,
                ProblemSeverity.HIGH, "desc"
            )
        );
    }

    @Test
    void problem_rejectsNullCategory() {
        assertThrows(
            IllegalArgumentException.class,
            () -> EvolutionProblem.of(
                "p1", null,
                ProblemSeverity.HIGH, "desc"
            )
        );
    }

    @Test
    void problem_rejectsNullSeverity() {
        assertThrows(
            IllegalArgumentException.class,
            () -> EvolutionProblem.of(
                "p1", ProblemCategory.CORRECTNESS,
                null, "desc"
            )
        );
    }

    @Test
    void problem_defensiveCopyEvidence() {
        var ev = EvidenceReference.of(
            EvidenceType.TRACE, "t1"
        );
        var list = new ArrayList<>(
            List.of(ev)
        );

        var problem = new EvolutionProblem(
            "p1", ProblemCategory.CORRECTNESS,
            ProblemSeverity.HIGH, list, "desc"
        );

        list.add(
            EvidenceReference.of(
                EvidenceType.EXECUTION, "l1"
            )
        );
        assertEquals(1, problem.evidence().size());
    }

    @Test
    void problem_unmodifiableEvidence() {
        var problem = EvolutionProblem.of(
            "p1", ProblemCategory.CORRECTNESS,
            ProblemSeverity.HIGH, "desc"
        );

        assertThrows(
            UnsupportedOperationException.class,
            () -> problem.evidence().add(null)
        );
    }

    @Test
    void problem_withEvidenceChaining() {
        var problem = EvolutionProblem.of(
            "p1", ProblemCategory.CORRECTNESS,
            ProblemSeverity.HIGH, "desc"
        );

        problem = problem.withEvidence(
            EvidenceReference.of(
                EvidenceType.TRACE, "t1"
            )
        );
        problem = problem.withEvidence(
            EvidenceReference.of(
                EvidenceType.EXECUTION, "l1"
            )
        );

        assertEquals(2, problem.evidence().size());
    }

    // ===== InMemoryEvolutionCandidate =====

    @Test
    void candidate_rejectsNullId() {
        var cs = CandidateChangeSet.of(
            List.of(Change.replace(
                "t", "a", "b", "r"
            )),
            ChangeScope.PROMPT
        );

        assertThrows(
            IllegalArgumentException.class,
            () -> InMemoryEvolutionCandidate.of(
                null, "agent", cs, List.of()
            )
        );
    }

    @Test
    void candidate_rejectsBlankBaseAgent() {
        var cs = CandidateChangeSet.of(
            List.of(Change.replace(
                "t", "a", "b", "r"
            )),
            ChangeScope.PROMPT
        );

        assertThrows(
            IllegalArgumentException.class,
            () -> InMemoryEvolutionCandidate.of(
                new CandidateId("c1"),
                " ", cs, List.of()
            )
        );
    }

    @Test
    void candidate_rejectsNullChangeSet() {
        assertThrows(
            IllegalArgumentException.class,
            () -> InMemoryEvolutionCandidate.of(
                new CandidateId("c1"),
                "agent", null, List.of()
            )
        );
    }

    @Test
    void candidate_defensiveCopyEvidence() {
        var cs = CandidateChangeSet.of(
            List.of(Change.replace(
                "t", "a", "b", "r"
            )),
            ChangeScope.PROMPT
        );
        var ev = EvidenceReference.of(
            EvidenceType.TRACE, "t1"
        );
        var list = new ArrayList<>(
            List.of(ev)
        );

        var cand = InMemoryEvolutionCandidate.of(
            new CandidateId("c1"),
            "agent", cs, list
        );

        list.add(
            EvidenceReference.of(
                EvidenceType.EXECUTION, "l1"
            )
        );
        assertEquals(1, cand.evidence().size());
    }

    @Test
    void candidate_unmodifiableEvidence() {
        var cs = CandidateChangeSet.of(
            List.of(Change.replace(
                "t", "a", "b", "r"
            )),
            ChangeScope.PROMPT
        );

        var cand = InMemoryEvolutionCandidate.of(
            new CandidateId("c1"),
            "agent", cs, List.of()
        );

        assertThrows(
            UnsupportedOperationException.class,
            () -> cand.evidence().add(null)
        );
    }

    @Test
    void candidate_unmodifiableMetadata() {
        var cs = CandidateChangeSet.of(
            List.of(Change.replace(
                "t", "a", "b", "r"
            )),
            ChangeScope.PROMPT
        );

        var cand = new InMemoryEvolutionCandidate(
            new CandidateId("c1"), "agent",
            cs, null, null, null, null, null
        );

        assertThrows(
            UnsupportedOperationException.class,
            () -> cand.metadata().put("k", "v")
        );
    }

    @Test
    void candidate_validatePreservesFields() {
        var cs = CandidateChangeSet.of(
            List.of(Change.replace(
                "t", "a", "b", "r"
            )),
            ChangeScope.PROMPT
        );

        var cand = InMemoryEvolutionCandidate.of(
            new CandidateId("c1"),
            "agent-a", cs, List.of()
        );

        var validated = cand.validate();
        assertEquals("c1", validated.id().value());
        assertEquals("agent-a", validated.baseAgent());
    }

    @Test
    void candidate_addMultipleEvidence() {
        var cs = CandidateChangeSet.of(
            List.of(Change.replace(
                "t", "a", "b", "r"
            )),
            ChangeScope.PROMPT
        );

        var cand = InMemoryEvolutionCandidate.of(
            new CandidateId("c1"),
            "agent", cs, List.of()
        );

        cand = cand.addEvidence(
            EvidenceReference.of(
                EvidenceType.TRACE, "t1"
            )
        );
        cand = cand.addEvidence(
            EvidenceReference.of(
                EvidenceType.EXECUTION, "l1"
            )
        );

        assertEquals(2, cand.evidence().size());
    }

    // ===== EvolutionApproval =====

    @Test
    void approval_rejectsBlankId() {
        assertThrows(
            IllegalArgumentException.class,
            () -> EvolutionApproval.of(
                " ", new CandidateId("c1"),
                "reviewer",
                ApprovalDecision.APPROVED,
                "ok"
            )
        );
    }

    @Test
    void approval_rejectsNullCandidateId() {
        assertThrows(
            IllegalArgumentException.class,
            () -> EvolutionApproval.of(
                "a1", null,
                "reviewer",
                ApprovalDecision.APPROVED,
                "ok"
            )
        );
    }

    @Test
    void approval_rejectsBlankReviewer() {
        assertThrows(
            IllegalArgumentException.class,
            () -> EvolutionApproval.of(
                "a1", new CandidateId("c1"),
                " ",
                ApprovalDecision.APPROVED,
                "ok"
            )
        );
    }

    @Test
    void approval_rejectsNullDecision() {
        assertThrows(
            IllegalArgumentException.class,
            () -> EvolutionApproval.of(
                "a1", new CandidateId("c1"),
                "reviewer", null, "ok"
            )
        );
    }

    // ===== CandidateLineage =====

    @Test
    void lineage_rejectsNullCandidateId() {
        assertThrows(
            IllegalArgumentException.class,
            () -> CandidateLineage.root(
                null, "agent"
            )
        );
    }

    @Test
    void lineage_rejectsBlankBaseAgent() {
        assertThrows(
            IllegalArgumentException.class,
            () -> CandidateLineage.root(
                new CandidateId("c1"), " "
            )
        );
    }

    @Test
    void lineage_defensiveCopyEvidence() {
        var ev = EvidenceReference.of(
            EvidenceType.TRACE, "t1"
        );
        var list = new ArrayList<>(
            List.of(ev)
        );

        var lineage = new CandidateLineage(
            new CandidateId("c1"),
            "agent", null, list
        );

        list.add(
            EvidenceReference.of(
                EvidenceType.EXECUTION, "l1"
            )
        );
        assertEquals(1, lineage.evidence().size());
    }

    @Test
    void lineage_unmodifiableEvidence() {
        var lineage = CandidateLineage.root(
            new CandidateId("c1"), "agent"
        );

        assertThrows(
            UnsupportedOperationException.class,
            () -> lineage.evidence().add(null)
        );
    }

    // ===== EvolutionPolicy =====

    @Test
    void policy_rejectsBlankId() {
        assertThrows(
            IllegalArgumentException.class,
            () -> EvolutionPolicy.defaults(" ")
        );
    }

    @Test
    void policy_rejectsNullId() {
        assertThrows(
            IllegalArgumentException.class,
            () -> EvolutionPolicy.defaults(null)
        );
    }

    @Test
    void policy_defensiveCopyScopes() {
        Set<ChangeScope> scopes = new HashSet<>();
        scopes.add(ChangeScope.PROMPT);

        var policy = new EvolutionPolicy(
            "p1", scopes, null,
            true, true, false
        );

        scopes.add(ChangeScope.MODEL);
        assertEquals(1, policy.allowedScopes().size());
    }

    @Test
    void policy_unmodifiableScopes() {
        var policy = EvolutionPolicy.defaults("p1");

        assertThrows(
            UnsupportedOperationException.class,
            () -> policy.allowedScopes()
                .add(ChangeScope.TOOL)
        );
        assertThrows(
            UnsupportedOperationException.class,
            () -> policy.requireHumanApproval()
                .add(ChangeScope.PROMPT)
        );
    }

    @Test
    void policy_customPolicy() {
        var policy = new EvolutionPolicy(
            "custom",
            Set.of(ChangeScope.PROMPT),
            Set.of(),
            false, false, true
        );

        assertTrue(
            policy.isAllowed(ChangeScope.PROMPT)
        );
        assertFalse(
            policy.isAllowed(ChangeScope.MODEL)
        );
        assertFalse(
            policy.needsApproval(ChangeScope.PROMPT)
        );
        assertTrue(
            policy.autoReleaseEnabled()
        );
    }

    // ===== ValidationResult =====

    @Test
    void validationResult_rejectsBlankGate() {
        assertThrows(
            IllegalArgumentException.class,
            () -> ValidationResult.pass(" ")
        );
    }

    @Test
    void validationResult_rejectsNullGate() {
        assertThrows(
            IllegalArgumentException.class,
            () -> ValidationResult.pass(null)
        );
    }

    @Test
    void validationResult_failHasMessage() {
        var result = ValidationResult.fail(
            "gate", "something broke"
        );

        assertFalse(result.passed());
        assertEquals("gate", result.gateName());
        assertEquals("something broke", result.message());
    }

    // ===== EvolutionContext =====

    @Test
    void context_rejectsBlankAgentRef() {
        assertThrows(
            IllegalArgumentException.class,
            () -> EvolutionContext.of(" ", "1.0")
        );
    }

    @Test
    void context_rejectsNullAgentRef() {
        assertThrows(
            IllegalArgumentException.class,
            () -> EvolutionContext.of(null, "1.0")
        );
    }

    @Test
    void context_defensiveCopyAttributes() {
        Map<String, String> attrs = new HashMap<>();
        attrs.put("a", "b");

        var ctx = new EvolutionContext(
            "agent", "1.0", null, attrs
        );

        attrs.put("c", "d");
        assertEquals(1, ctx.attributes().size());
    }

    @Test
    void context_unmodifiableAttributes() {
        var ctx = EvolutionContext.of(
            "agent", "1.0"
        );

        assertThrows(
            UnsupportedOperationException.class,
            () -> ctx.attributes().put("k", "v")
        );
    }

    // ===== DefaultEvolutionLoop =====

    @Test
    void loop_emptyGeneratorThrows() {
        var generator = new CandidateGenerator() {
            @Override
            public List<EvolutionCandidate> generate(
                EvolutionProblem problem,
                EvolutionContext ctx
            ) {
                return List.of();
            }
        };

        var loop = new DefaultEvolutionLoop(
            generator
        );

        var problem = EvolutionProblem.of(
            "p1", ProblemCategory.CORRECTNESS,
            ProblemSeverity.HIGH, "desc"
        );
        var ctx = EvolutionContext.of(
            "agent", "1.0"
        );

        assertThrows(
            IllegalStateException.class,
            () -> loop.execute(
                problem, ctx, List.of()
            )
        );
    }

    @Test
    void loop_noGatesApproves() {
        var generator = new CandidateGenerator() {
            @Override
            public List<EvolutionCandidate> generate(
                EvolutionProblem problem,
                EvolutionContext ctx
            ) {
                var cs = CandidateChangeSet.of(
                    List.of(Change.replace(
                        "t", "a", "b", "r"
                    )),
                    ChangeScope.PROMPT
                );
                return List.of(
                    InMemoryEvolutionCandidate.of(
                        new CandidateId("g1"),
                        ctx.agentReference(),
                        cs, List.of()
                    )
                );
            }
        };

        var loop = new DefaultEvolutionLoop(
            generator
        );

        var problem = EvolutionProblem.of(
            "p1", ProblemCategory.CORRECTNESS,
            ProblemSeverity.HIGH, "desc"
        );
        var ctx = EvolutionContext.of(
            "agent", "1.0"
        );

        var result = loop.execute(
            problem, ctx, List.of()
        );

        assertEquals(
            CandidateStatus.APPROVED,
            result.status()
        );
    }
}
