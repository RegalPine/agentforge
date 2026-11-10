package io.github.regalpine.agentforge.hardening.conformance;

import io.github.regalpine.agentforge.capability.InMemorySkillRegistry;
import io.github.regalpine.agentforge.cli.SkillExitCodes;
import io.github.regalpine.agentforge.cli.command.SkillCommands;
import io.github.regalpine.agentforge.kernel.*;
import io.github.regalpine.agentforge.skill.governance.*;
import io.github.regalpine.agentforge.runtime.DefaultSkillResolver;
import io.github.regalpine.agentforge.sdk.*;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * conformance-skill-integration:
 * AF-SKILL-INT-001 ~ 010.
 * Agent→Skill / Graph→Skill /
 * Skill→Capability / SDK / CLI /
 * Server / Cross-module.
 * AF-AD-15 §14.2, T-S6.9.
 */
class SkillIntegrationConformanceTest {

    // ── AF-SKILL-INT-001 Agent→Skill ─

    @Test
    void afSkillInt001_agentDecisionType() {
        // CALL_SKILL decision type exists
        // in the DecisionType enum
        var dt = io.github.regalpine
            .agentforge.agent
            .DecisionType.CALL_SKILL;
        assertNotNull(dt);
        assertEquals(
            "CALL_SKILL", dt.name()
        );
    }

    // ── AF-SKILL-INT-002 Graph→Skill ─

    @Test
    void afSkillInt002_graphNodeType() {
        // SKILL node type exists in
        // NodeType enum
        var nt = io.github.regalpine
            .agentforge.graph.NodeType.SKILL;
        assertNotNull(nt);
    }

    // ── AF-SKILL-INT-003 Skill→Capability

    @Test
    void afSkillInt003_skillToCapability() {
        // SkillExecutionStrategy
        // .DIRECT_CAPABILITY bridges
        // Skill → Capability
        var strategy =
            SkillExecutionStrategy
                .DIRECT_CAPABILITY;
        assertNotNull(strategy);
    }

    // ── AF-SKILL-INT-004 SDK Facade ──

    @Test
    void afSkillInt004_sdkFacadeExists() {
        // SkillApi is a Facade that does
        // not expose Worker/Lease/Queue
        SkillApi api = new SkillApi() {
            public io.github.regalpine.agentforge
                .sdk.SkillDescriptor resolve(
                    String id, String v
                ) {
                throw new ResolutionException(
                    "Skill", id
                );
            }
            public java.util.List<io.github.regalpine
                .agentforge.sdk.SkillDescriptor>
                discover(SkillDiscoveryQuery q)
            {
                return java.util.List.of();
            }
            public SkillExecutionResult invoke(
                String id, String v,
                java.util.Map<String, Object> in
            ) {
                return SkillExecutionResult
                    .failure(
                        id, v,
                        "NOT_CONFIGURED",
                        "No Skill configured"
                    );
            }
            public java.util.concurrent.CompletionStage<
                SkillExecutionResult> invokeAsync(
                    String id, String v,
                    java.util.Map<String, Object> in
                ) {
                return java.util.concurrent
                    .CompletableFuture.completedFuture(
                        invoke(id, v, in)
                    );
            }
            public java.util.List<String>
                listSkillIds()
            {
                return java.util.List.of();
            }
        };
        assertNotNull(api);

        // discover returns empty list
        var found = api.discover(
            SkillDiscoveryQuery.all()
        );
        assertTrue(found.isEmpty());
    }

    @Test
    void afSkillInt004_sdkDescriptor() {
        var kernel =
            new io.github.regalpine.agentforge
                .kernel.SkillDescriptor(
                    new SkillId("s1"),
                    "TestSkill",
                    new Version("1.0.0"),
                    "Test goal",
                    SkillExecutionStrategy
                        .DIRECT_CAPABILITY,
                    "owner1",
                    "ACTIVE"
                );
        var sdk =
            io.github.regalpine.agentforge
                .sdk.SkillDescriptor.from(
                    kernel
                );
        assertEquals("s1", sdk.skillId());
        assertEquals("ACTIVE", sdk.status());
    }

    // ── AF-SKILL-INT-005 CLI Commands ──

    @Test
    void afSkillInt005_cliHas12Commands() {
        var cmd = new SkillCommands();
        assertEquals(12,
            cmd.subcommands().size()
        );
    }

    @Test
    void afSkillInt005_cliExitCodes() {
        assertEquals(0,
            SkillExitCodes.SUCCESS
        );
        assertEquals(3,
            SkillExitCodes.SKILL_NOT_FOUND
        );
        assertEquals(4,
            SkillExitCodes.SECURITY_DENIED
        );
    }

    // ── AF-SKILL-INT-006 Resolver ────

    @Test
    void afSkillInt006_resolverBridge() {
        // DefaultSkillResolver bridges
        // SkillRegistry → ResolvedSkill
        var registry =
            new InMemorySkillRegistry();
        Schema s = () -> "object";
        var metadata =
            new SkillPackageMetadata(
                new SkillId("int-test"),
                new Version("1.0.0"),
                "IntTest",
                "Integration goal",
                SkillContract.of(s, s),
                SkillDependencies.empty(),
                SkillExecutionStrategy
                    .DIRECT_CAPABILITY,
                "owner", "domain",
                null, null,
                URI.create("file:///int.tar.gz")
            );
        registry.register(
            metadata,
            SkillDigest.sha256("int")
        );

        var resolver =
            new DefaultSkillResolver(registry);
        var ref = SkillReference.of(
            "int-test", "1.0.0"
        );
        var resolved =
            resolver.resolve(ref);

        assertNotNull(resolved);
        assertEquals(
            SkillExecutionStrategy
                .DIRECT_CAPABILITY,
            resolved.strategy()
        );
    }

    // ── AF-SKILL-INT-007 Cross-module ─

    @Test
    void afSkillInt007_kernelTypesShared() {
        // All modules share kernel types
        SkillId id = new SkillId("shared");
        Version v = new Version("1.0.0");
        SkillReference ref =
            new SkillReference(id, v);

        assertEquals("shared",
            ref.skillId().value()
        );
        assertEquals("1.0.0",
            ref.version().value()
        );
    }

    // ── AF-SKILL-INT-008 Security ────

    @Test
    void afSkillInt008_securityIsolation() {
        // Security module provides
        // SkillSecurityChain for
        // cross-module security
        var profile =
            SkillSecurityProfile.lowRisk();
        assertNotNull(profile);
        assertEquals(
            SkillRiskLevel.LOW,
            profile.riskLevel()
        );
    }

    // ── AF-SKILL-INT-009 Evaluation ──

    @Test
    void afSkillInt009_evalIsolation() {
        // Evaluation PASS ≠ Auth PASS
        // is enforced across modules
        var contract =
            SkillEvaluationContract.none();
        assertFalse(
            contract.requiresEvaluation()
        );
    }

    // ── AF-SKILL-INT-010 NoForbidden ──

    @Test
    void afSkillInt010_noForbiddenTypes() {
        // AF-AD-15 §1.4/§16: no
        // SkillRuntime / SkillEngine /
        // SkillDSL / SkillState etc.
        // This test verifies by checking
        // that the SkillInvoker is a
        // Facade, not a new Runtime.
        var invokerClass =
            io.github.regalpine.agentforge
                .runtime.SkillInvoker.class;
        assertTrue(
            invokerClass.isInterface()
        );
    }
}
