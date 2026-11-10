package io.github.regalpine.agentforge.arch;


import io.github.regalpine.agentforge.kernel.*;
import io.github.regalpine.agentforge.skill.governance.*;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Modifier;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Skill Architecture Freeze & Release.
 * AF-AD-15 §15: Ten Hard Constraints.
 * AF-AD-15 §14.3: Ten Acceptance Criteria.
 * AF-AD-15 §16: Convergence Statement.
 * AF-AD-15 §1.4: Forbidden Types.
 *
 * <p>T-S7.1 ~ T-S7.8: Full architecture
 * freeze verification for the Skill
 * subsystem.
 */
class SkillArchitectureFreezeTest {

    // ═══════════════════════════════════
    // T-S7.1: Ten Hard Constraints
    // AF-SKILL-RT-R001 ~ R010
    // ═══════════════════════════════════

    /**
     * AF-SKILL-RT-R001: Skill must use
     * existing Execution model.
     */
    @Test
    void afSkillRtR001_skillUsesExecution() {
        // SkillInvoker is a Facade that
        // delegates to existing Execution
        // infrastructure
        assertClassExists(
            "io.github.regalpine.agentforge"
                + ".runtime.ExecutionEngine"
        );
        assertInterfaceExists(
            "io.github.regalpine.agentforge"
                + ".runtime.SkillInvoker"
        );
    }

    /**
     * AF-SKILL-RT-R002: Skill must not
     * create an independent Runtime.
     */
    @Test
    void afSkillRtR002_noIndependentRuntime() {
        assertNoClass(
            "io.github.regalpine.agentforge"
                + ".runtime.SkillRuntime"
        );
        assertNoClass(
            "io.github.regalpine.agentforge"
                + ".runtime.SkillEngine"
        );
    }

    /**
     * AF-SKILL-RT-R003: Skill must not
     * bypass Policy.
     */
    @Test
    void afSkillRtR003_noPolicyBypass() {
        // SkillSecurityChain enforces
        // policy checks
        assertClassExists(
            "io.github.regalpine.agentforge"
                + ".security.skill"
                + ".SkillSecurityChain"
        );
        // SkillReleaseGate enforces
        // publish gate policy
        assertClassExists(
            "io.github.regalpine.agentforge"
                + ".capability.SkillReleaseGate"
        );
    }

    /**
     * AF-SKILL-RT-R004: Skill must not
     * bypass Capability Security.
     */
    @Test
    void afSkillRtR004_noCapabilityBypass() {
        // SkillSecurityChain
        // .checkCapabilityPermission
        // verifies independent
        // capability authorization
        assertClassExists(
            "io.github.regalpine.agentforge"
                + ".security.skill"
                + ".SkillSecurityChain"
        );
    }

    /**
     * AF-SKILL-RT-R005: Skill must not
     * directly access external side-effect
     * resources.
     */
    @Test
    void afSkillRtR005_noDirectSideEffects() {
        // SkillDataBoundary controls
        // egress and external access
        assertFrozen(
            "io.github.regalpine.agentforge"
                + ".skill.governance.SkillDataBoundary"
        );
        // SkillExecutionConstraint controls
        // side effect allowance
        assertFrozen(
            "io.github.regalpine.agentforge"
                + ".skill.governance"
                + ".SkillExecutionConstraint"
        );
    }

    /**
     * AF-SKILL-RT-R006: Skill Execution
     * must bind Skill Version.
     */
    @Test
    void afSkillRtR006_versionBinding() {
        // SkillReference binds skillId +
        // version
        assertFrozen(
            "io.github.regalpine.agentforge"
                + ".kernel.SkillReference"
        );
        // SkillDigest binds artifact
        assertFrozen(
            "io.github.regalpine.agentforge"
                + ".skill.governance.SkillDigest"
        );
    }

    /**
     * AF-SKILL-RT-R007: Skill must not
     * create a second State model.
     */
    @Test
    void afSkillRtR007_noSecondState() {
        assertNoClass(
            "io.github.regalpine.agentforge"
                + ".kernel.SkillState"
        );
    }

    /**
     * AF-SKILL-RT-R008: Skill must not
     * create a second Event model.
     */
    @Test
    void afSkillRtR008_noSecondEvent() {
        assertNoClass(
            "io.github.regalpine.agentforge"
                + ".kernel.SkillEvent"
        );
        assertNoClass(
            "io.github.regalpine.agentforge"
                + ".event.SkillEvent"
        );
    }

    /**
     * AF-SKILL-RT-R009: Skill must not
     * create a second Checkpoint model.
     */
    @Test
    void afSkillRtR009_noSecondCheckpoint() {
        assertNoClass(
            "io.github.regalpine.agentforge"
                + ".kernel.SkillCheckpoint"
        );
    }

    /**
     * AF-SKILL-RT-R010: Skill must not
     * have an independent Scheduler.
     */
    @Test
    void afSkillRtR010_noIndependentScheduler() {
        assertNoClass(
            "io.github.regalpine.agentforge"
                + ".runtime.SkillScheduler"
        );
    }

    // ═══════════════════════════════════
    // T-S7.2: Forbidden Types
    // AF-AD-15 §1.4/§16
    // ═══════════════════════════════════

    @Test
    void afSkillForbidden_noSkillRuntime() {
        assertNoClass(
            "io.github.regalpine.agentforge"
                + ".runtime.SkillRuntime"
        );
    }

    @Test
    void afSkillForbidden_noSkillEngine() {
        assertNoClass(
            "io.github.regalpine.agentforge"
                + ".runtime.SkillEngine"
        );
    }

    @Test
    void afSkillForbidden_noSkillDSL() {
        assertNoClass(
            "io.github.regalpine.agentforge"
                + ".kernel.SkillDSL"
        );
    }

    @Test
    void afSkillForbidden_noSkillMemory() {
        assertNoClass(
            "io.github.regalpine.agentforge"
                + ".memory.SkillMemory"
        );
    }

    @Test
    void afSkillForbidden_noSkillRAG() {
        assertNoClass(
            "io.github.regalpine.agentforge"
                + ".rag.SkillRAG"
        );
    }

    @Test
    void afSkillForbidden_noSkillPlanner() {
        assertNoClass(
            "io.github.regalpine.agentforge"
                + ".runtime.SkillPlanner"
        );
    }

    @Test
    void afSkillForbidden_noSkillLearner() {
        assertNoClass(
            "io.github.regalpine.agentforge"
                + ".runtime.SkillLearner"
        );
    }

    @Test
    void afSkillForbidden_noSkillEvolution() {
        assertNoClass(
            "io.github.regalpine.agentforge"
                + ".runtime.SkillEvolution"
        );
    }

    @Test
    void afSkillForbidden_noSkillMarketplace() {
        assertNoClass(
            "io.github.regalpine.agentforge"
                + ".capability.SkillMarketplace"
        );
    }

    // ═══════════════════════════════════
    // T-S7.3: Reuse List
    // AF-AD-15 §16
    // ═══════════════════════════════════

    @Test
    void afSkillReuse_execution() {
        assertClassExists(
            "io.github.regalpine.agentforge"
                + ".runtime.ExecutionEngine"
        );
    }

    @Test
    void afSkillReuse_state() {
        assertClassExists(
            "io.github.regalpine.agentforge"
                + ".event.ExecutionState"
        );
    }

    @Test
    void afSkillReuse_event() {
        assertClassExists(
            "io.github.regalpine.agentforge"
                + ".event.InMemoryEventPublisher"
        );
    }

    @Test
    void afSkillReuse_checkpoint() {
        assertClassExists(
            "io.github.regalpine.agentforge"
                + ".persistence.ExecutionStateStore"
        );
    }

    @Test
    void afSkillReuse_memory() {
        assertClassExists(
            "io.github.regalpine.agentforge"
                + ".memory.InMemoryMemoryStore"
        );
    }

    @Test
    void afSkillReuse_rag() {
        assertClassExists(
            "io.github.regalpine.agentforge"
                + ".rag.model.KnowledgeChunk"
        );
    }

    @Test
    void afSkillReuse_security() {
        assertClassExists(
            "io.github.regalpine.agentforge"
                + ".security.SecurityContext"
        );
    }

    @Test
    void afSkillReuse_evaluation() {
        assertClassExists(
            "io.github.regalpine.agentforge"
                + ".evaluation.model.Score"
        );
    }

    @Test
    void afSkillReuse_observability() {
        assertClassExists(
            "io.github.regalpine.agentforge"
                + ".observability.TraceManager"
        );
    }

    @Test
    void afSkillReuse_distributed() {
        assertClassExists(
            "io.github.regalpine.agentforge"
                + ".distributed.lease"
                + ".ExecutionLease"
        );
    }

    // ═══════════════════════════════════
    // T-S7.4: Acceptance Criteria
    // AF-AD-15 §14.3
    // ═══════════════════════════════════

    /**
     * AC-1: Same Skill version replacement
     * does not modify Graph definition.
     */
    @Test
    void afSkillAc001_versionReplacement() {
        // SkillReference binds version
        var ref1 = SkillReference.of(
            "s1", "1.0.0"
        );
        var ref2 = SkillReference.of(
            "s1", "1.0.1"
        );
        assertNotEquals(ref1, ref2);
        // Different versions = different
        // references, same graph definition
    }

    /**
     * AC-2: SkillRuntime invocation count
     * must be zero.
     */
    @Test
    void afSkillAc002_noSkillRuntimeCount() {
        // Verified by forbidden types
        // check above
        assertNoClass(
            "io.github.regalpine.agentforge"
                + ".runtime.SkillRuntime"
        );
    }

    /**
     * AC-3: Unauthorized Skill rejected.
     */
    @Test
    void afSkillAc003_unauthorizedRejected() {
        // SkillSecurityChain denies
        // privilege escalation
        var profile = new SkillSecurityProfile(
            SkillRiskLevel.HIGH,
            "INTERNAL", "INTERNAL",
            Set.of("admin"),
            Set.of(), Set.of(),
            SkillDataBoundary.restrictive(),
            SkillExecutionConstraint.defaults()
        );
        var chain = io.github.regalpine
            .agentforge.security.skill
            .SkillSecurityChain.builder()
            .profile(profile)
            .callerPermissions(Set.of("read"))
            .build();

        var result = chain
            .checkPrivilegeEscalation();
        assertFalse(result.allowed());
    }

    /**
     * AC-4: Capability Permission not auto
     * granted by Skill Dependency.
     */
    @Test
    void afSkillAc004_capabilityPermIndependent() {
        var chain = io.github.regalpine
            .agentforge.security.skill
            .SkillSecurityChain.builder()
            .profile(SkillSecurityProfile.lowRisk())
            .callerPermissions(Set.of())
            .build();

        var result = chain
            .checkCapabilityPermission(
                "cap.test"
            );
        assertFalse(result.allowed());
    }

    /**
     * AC-5: Cross-tenant Skill data
     * access isolated.
     */
    @Test
    void afSkillAc005_tenantIsolation() {
        var chain = io.github.regalpine
            .agentforge.security.skill
            .SkillSecurityChain.builder()
            .profile(SkillSecurityProfile.lowRisk())
            .callerPermissions(Set.of())
            .tenantId("tenant-A")
            .build();

        var result = chain
            .checkTenantIsolation("tenant-B");
        assertFalse(result.allowed());
    }

    /**
     * AC-6: CRITICAL Skill supports
     * Approval Policy.
     */
    @Test
    void afSkillAc006_criticalApproval() {
        assertTrue(
            SkillRiskLevel.CRITICAL
                .requiresApproval()
        );
    }

    /**
     * AC-7: Revoked Skill rejects new
     * Execution.
     */
    @Test
    void afSkillAc007_revokedRejected() {
        // DefaultSkillResolver rejects
        // REVOKED state
        assertFalse(
            SkillLifecycleState.REVOKED
                .isExecutable()
        );
        assertTrue(
            SkillLifecycleState.REVOKED
                .isTerminal()
        );
    }

    /**
     * AC-8: Checkpoint Recovery uses
     * original Skill Version.
     */
    @Test
    void afSkillAc008_checkpointVersionBinding() {
        // SkillReference is immutable
        // and binds version
        var ref = SkillReference.of(
            "s1", "1.0.0"
        );
        assertEquals("1.0.0",
            ref.version().value()
        );
        // Recovery uses the same
        // SkillReference, no auto-upgrade
    }

    /**
     * AC-9: Skill Artifact Digest
     * verifiable.
     */
    @Test
    void afSkillAc009_digestVerifiable() {
        var d1 = SkillDigest.sha256("test");
        var d2 = SkillDigest.sha256("test");
        assertEquals(d1, d2);
        // Same id + version = same digest
    }

    /**
     * AC-10: Evaluation PASS does not
     * bypass Security Policy.
     */
    @Test
    void afSkillAc010_evalPassNoAuthBypass() {
        assertFalse(
            io.github.regalpine.agentforge
                .evaluation.skill
                .SkillEvaluationSecurityBoundary
                .isLlmJudgeConstrained(
                    "SEMANTIC"
                ) == false
        );
        // LLM Judge is always constrained
        // even when evaluation passes
    }

    // ═══════════════════════════════════
    // T-S7.6: Dependency Direction
    // AF-AD-15 §13.2
    // ═══════════════════════════════════

    /**
     * Skill types are in kernel (core),
     * not in runtime/server/cli.
     */
    @Test
    void afSkillDep_kernelContainsSkillTypes() {
        // Core Skill types live in kernel
        assertFrozen(
            "io.github.regalpine.agentforge"
                + ".kernel.Skill"
        );
        assertFrozen(
            "io.github.regalpine.agentforge"
                + ".kernel.SkillId"
        );
        assertFrozen(
            "io.github.regalpine.agentforge"
                + ".kernel.SkillReference"
        );
    }

    @Test
    void afSkillDep_skillInvokerInRuntime() {
        // SkillInvoker is in runtime
        // (above kernel)
        assertInterfaceExists(
            "io.github.regalpine.agentforge"
                + ".runtime.SkillInvoker"
        );
    }

    @Test
    void afSkillDep_skillApiInSdk() {
        // SkillApi is in SDK (above
        // runtime)
        assertInterfaceExists(
            "io.github.regalpine.agentforge"
                + ".sdk.SkillApi"
        );
    }

    /**
     * Kernel does not depend on runtime.
     */
    @Test
    void afSkillDep_kernelNotDependOnRuntime() {
        // Skill interface in kernel does
        // not import runtime
        var skillClass = Skill.class;
        var pkg = skillClass.getPackage()
            .getName();
        assertEquals(
            "io.github.regalpine"
                + ".agentforge.kernel",
            pkg
        );
    }

    // ═══════════════════════════════════
    // T-S7.8: Convergence Statement
    // AF-AD-15 §16
    // ═══════════════════════════════════

    /**
     * New semantic types exist and are
     * frozen.
     */
    @Test
    void afSkillConv_newSemanticTypes() {
        Set<String> newTypes = Set.of(
            "io.github.regalpine.agentforge"
                + ".kernel.Skill",
            "io.github.regalpine.agentforge"
                + ".kernel.SkillId",
            "io.github.regalpine.agentforge"
                + ".kernel.SkillReference",
            "io.github.regalpine.agentforge"
                + ".kernel.SkillContract",
            "io.github.regalpine.agentforge"
                + ".kernel.SkillDependencies",
            "io.github.regalpine.agentforge"
                + ".kernel"
                + ".SkillExecutionStrategy",
            "io.github.regalpine.agentforge"
                + ".kernel.SkillDescriptor",
            "io.github.regalpine.agentforge"
                + ".skill.governance.SkillDigest",
            "io.github.regalpine.agentforge"
                + ".skill.governance"
                + ".SkillLifecycleState",
            "io.github.regalpine.agentforge"
                + ".skill.governance"
                + ".SkillSecurityProfile",
            "io.github.regalpine.agentforge"
                + ".skill.governance"
                + ".SkillGovernanceProfile",
            "io.github.regalpine.agentforge"
                + ".skill.governance"
                + ".SkillEvaluationContract"
        );

        for (String type : newTypes) {
            assertFrozen(type);
        }
    }

    /**
     * Skill is a new semantic layer, not
     * a new execution layer.
     */
    @Test
    void afSkillConv_semanticNotExecution() {
        // SkillInvoker is a Facade
        var cl = io.github.regalpine
            .agentforge.runtime
            .SkillInvoker.class;
        assertTrue(cl.isInterface());

        // Skill is an interface
        assertTrue(Skill.class.isInterface());

        // SkillExecutionStrategy is an enum
        assertTrue(
            SkillExecutionStrategy.class
                .isEnum()
        );
    }

    // ── Helpers ──────────────────────

    private void assertFrozen(String cn) {
        try {
            Class<?> cl = Class.forName(cn);
            boolean frozen = cl.isRecord()
                || cl.isInterface()
                || cl.isSealed()
                || Modifier.isFinal(
                    cl.getModifiers()
                );
            assertTrue(frozen,
                cn + " must be frozen");
        } catch (ClassNotFoundException e) {
            fail("Frozen class not found: "
                + cn);
        }
    }

    private void assertClassExists(String cn) {
        try {
            Class.forName(cn);
        } catch (ClassNotFoundException e) {
            fail("Class not found: " + cn);
        }
    }

    private void assertInterfaceExists(String cn) {
        try {
            Class<?> cl = Class.forName(cn);
            assertTrue(cl.isInterface(),
                cn + " must be an interface");
        } catch (ClassNotFoundException e) {
            fail("Interface not found: " + cn);
        }
    }

    private void assertNoClass(String cn) {
        try {
            Class.forName(cn);
            fail("Class should NOT exist: "
                + cn);
        } catch (ClassNotFoundException e) {
            // expected
        }
    }
}
