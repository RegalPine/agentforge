package io.github.regalpine.agentforge.hardening.conformance;

import io.github.regalpine.agentforge.capability.InMemorySkillRegistry;
import io.github.regalpine.agentforge.kernel.*;
import io.github.regalpine.agentforge.skill.governance.*;
import io.github.regalpine.agentforge.runtime.DefaultSkillResolver;
import io.github.regalpine.agentforge.runtime.SkillResolutionException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * conformance-skill-runtime:
 * AF-SKILL-RT-001 ~ 025.
 * Resolution / Execution / Strategy /
 * Cycle / Timeout / Budget / Error Code.
 * AF-AD-15 §14.2, T-S6.4~T-S6.5.
 */
class SkillRuntimeConformanceTest {

    private InMemorySkillRegistry registry;
    private DefaultSkillResolver resolver;

    @BeforeEach
    void setUp() {
        registry = new InMemorySkillRegistry();
        Schema s = () -> "object";
        var metadata = new SkillPackageMetadata(
            new SkillId("rt-test"),
            new Version("1.0.0"),
            "RtTest Skill",
            "Runtime test goal",
            SkillContract.of(s, s),
            SkillDependencies.empty(),
            SkillExecutionStrategy
                .DIRECT_CAPABILITY,
            "owner1", "domain1",
            null, null,
            URI.create("file:///rt.tar.gz")
        );
        var digest = SkillDigest.sha256("rt");
        registry.register(metadata, digest);
        resolver = new DefaultSkillResolver(registry);
    }

    // ── AF-SKILL-RT-001 Resolution ─

    @Test
    void afSkillRt001_resolveSuccess() {
        var ref = SkillReference.of(
            "rt-test", "1.0.0"
        );
        var resolved = resolver.resolve(ref);
        assertNotNull(resolved);
        assertEquals(
            SkillExecutionStrategy
                .DIRECT_CAPABILITY,
            resolved.strategy()
        );
    }

    @Test
    void afSkillRt001_resolveNotFound() {
        var ref = SkillReference.of(
            "nonexistent", "1.0.0"
        );
        assertThrows(
            SkillResolutionException.class,
            () -> resolver.resolve(ref)
        );
    }

    // ── AF-SKILL-RT-002 Revoked ────

    @Test
    void afSkillRt002_revokedSkillRejected() {
        var ref = SkillReference.of(
            "rt-test", "1.0.0"
        );
        // Follow full lifecycle chain:
        // DRAFT → VALIDATED → TESTED →
        // EVALUATED → APPROVED → PUBLISHED
        // → ACTIVE → REVOKED
        registry.transition(
            ref, SkillLifecycleState.VALIDATED
        );
        registry.transition(
            ref, SkillLifecycleState.TESTED
        );
        registry.transition(
            ref, SkillLifecycleState.EVALUATED
        );
        registry.transition(
            ref, SkillLifecycleState.APPROVED
        );
        registry.transition(
            ref, SkillLifecycleState.PUBLISHED
        );
        registry.transition(
            ref, SkillLifecycleState.ACTIVE
        );
        registry.transition(
            ref, SkillLifecycleState.REVOKED
        );

        assertThrows(
            SkillResolutionException.class,
            () -> resolver.resolve(ref)
        );
    }

    // ── AF-SKILL-RT-003 Invocation ─

    @Test
    void afSkillRt003_invocationRequest() {
        var ref = SkillReference.of(
            "rt-test", "1.0.0"
        );
        var req = new SkillInvocationRequest(
            ref, Map.of("key", "val"),
            "caller1", "tenant1",
            30000L
        );
        assertEquals("caller1",
            req.callerId());
        assertEquals("tenant1",
            req.tenantId());
        assertEquals(30000L,
            req.timeoutMillis());
    }

    @Test
    void afSkillRt003_invocationDefaults() {
        var ref = SkillReference.of(
            "rt-test", "1.0.0"
        );
        var req = new SkillInvocationRequest(
            ref, Map.of()
        );
        assertEquals(30000L,
            req.timeoutMillis());
        assertNull(req.callerId());
    }

    // ── AF-SKILL-RT-004 Strategy ───

    @Test
    void afSkillRt004_strategyBinding() {
        var ref = SkillReference.of(
            "rt-test", "1.0.0"
        );
        var resolved = resolver.resolve(ref);
        assertEquals(
            SkillExecutionStrategy
                .DIRECT_CAPABILITY,
            resolved.strategy()
        );
    }

    // ── AF-SKILL-RT-005 Result ─────

    @Test
    void afSkillRt005_successResult() {
        var ref = SkillReference.of(
            "rt-test", "1.0.0"
        );
        var result = SkillResult.success(
            ref, Map.of("out", "data")
        );
        assertTrue(result.isSuccess());
        assertEquals(
            SkillResultStatus.COMPLETED,
            result.status()
        );
        assertEquals("data",
            result.output().get("out"));
    }

    @Test
    void afSkillRt005_failedResult() {
        var ref = SkillReference.of(
            "rt-test", "1.0.0"
        );
        var result = SkillResult.failed(
            ref,
            SkillErrorCode
                .SKILL_EXECUTION_FAILED,
            "Something went wrong"
        );
        assertFalse(result.isSuccess());
        assertEquals(
            SkillResultStatus.FAILED,
            result.status()
        );
    }

    // ── AF-SKILL-RT-006 Timeout ────

    @Test
    void afSkillRt006_timeoutConstraint() {
        var constraint =
            SkillExecutionConstraint.defaults();
        assertTrue(
            constraint.timeout().toMillis() > 0
        );
    }

    @Test
    void afSkillRt006_customTimeout() {
        var constraint =
            new SkillExecutionConstraint(
                java.time.Duration.ofSeconds(60),
                3, 1000L, 500L, true
            );
        assertEquals(60,
            constraint.timeout().getSeconds()
        );
        assertEquals(3,
            constraint.maxRetries()
        );
    }

    // ── AF-SKILL-RT-007 Budget ─────

    @Test
    void afSkillRt007_budgetConstraint() {
        var constraint =
            new SkillExecutionConstraint(
                java.time.Duration.ofSeconds(30),
                0, 500L, 1000L, false
            );
        assertEquals(500L,
            constraint.tokenBudget()
        );
        assertEquals(1000L,
            constraint.costBudgetMicros()
        );
        assertFalse(
            constraint.allowsSideEffects()
        );
    }

    // ── AF-SKILL-RT-008 Error Codes ─

    @Test
    void afSkillRt008_errorCodesExist() {
        assertNotNull(
            SkillErrorCode.SKILL_NOT_FOUND
        );
        assertNotNull(
            SkillErrorCode.SKILL_POLICY_DENIED
        );
        assertNotNull(
            SkillErrorCode
                .SKILL_EXECUTION_FAILED
        );
    }

    // ── AF-SKILL-RT-009 Immutability ─

    @Test
    void afSkillRt009_requestDefensiveCopy() {
        var ref = SkillReference.of(
            "rt-test", "1.0.0"
        );
        var input =
            new java.util.HashMap<String, Object>(
                Map.of("k", "v")
            );
        var req = new SkillInvocationRequest(
            ref, input,
            "c", "t", 5000L
        );
        input.put("k2", "v2");
        assertEquals(1,
            req.input().size()
        );
    }

    @Test
    void afSkillRt009_resultDefensiveCopy() {
        var ref = SkillReference.of(
            "rt-test", "1.0.0"
        );
        var output =
            new java.util.HashMap<String, Object>(
                Map.of("k", "v")
            );
        var result = SkillResult.success(
            ref, output
        );
        output.put("k2", "v2");
        assertEquals(1,
            result.output().size()
        );
    }
}
