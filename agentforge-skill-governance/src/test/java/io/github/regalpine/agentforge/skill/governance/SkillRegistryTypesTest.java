package io.github.regalpine.agentforge.skill.governance;

import io.github.regalpine.agentforge.kernel.Schema;
import io.github.regalpine.agentforge.kernel.Skill;
import io.github.regalpine.agentforge.kernel.SkillContract;
import io.github.regalpine.agentforge.kernel.SkillDefinition;
import io.github.regalpine.agentforge.kernel.SkillDependencies;
import io.github.regalpine.agentforge.kernel.SkillDescriptor;
import io.github.regalpine.agentforge.kernel.SkillExecutionStrategy;
import io.github.regalpine.agentforge.kernel.SkillId;
import io.github.regalpine.agentforge.kernel.Version;


import org.junit.jupiter.api.Test;

import java.net.URI;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Skill Registry 类型测试.
 * T-S1.16: 生命周期状态机 / Digest /
 * PackageMetadata / Query / Registration.
 */
class SkillRegistryTypesTest {

    // ── SkillLifecycleState ──────────

    @Test
    void shouldAllowDraftToValidated() {
        assertTrue(
            SkillLifecycleState.DRAFT
                .canTransitionTo(
                    SkillLifecycleState.VALIDATED
                )
        );
    }

    @Test
    void shouldRejectDraftToActive() {
        assertFalse(
            SkillLifecycleState.DRAFT
                .canTransitionTo(
                    SkillLifecycleState.ACTIVE
                )
        );
    }

    @Test
    void shouldRejectDraftToPublished() {
        assertFalse(
            SkillLifecycleState.DRAFT
                .canTransitionTo(
                    SkillLifecycleState.PUBLISHED
                )
        );
    }

    @Test
    void shouldTransitionFullLifecycle() {
        SkillLifecycleState s =
            SkillLifecycleState.DRAFT;

        s = s.transitionTo(
            SkillLifecycleState.VALIDATED);
        assertEquals(
            SkillLifecycleState.VALIDATED, s);

        s = s.transitionTo(
            SkillLifecycleState.TESTED);
        assertEquals(
            SkillLifecycleState.TESTED, s);

        s = s.transitionTo(
            SkillLifecycleState.EVALUATED);
        assertEquals(
            SkillLifecycleState.EVALUATED, s);

        s = s.transitionTo(
            SkillLifecycleState.APPROVED);
        assertEquals(
            SkillLifecycleState.APPROVED, s);

        s = s.transitionTo(
            SkillLifecycleState.PUBLISHED);
        assertEquals(
            SkillLifecycleState.PUBLISHED, s);

        s = s.transitionTo(
            SkillLifecycleState.ACTIVE);
        assertEquals(
            SkillLifecycleState.ACTIVE, s);
    }

    @Test
    void shouldRejectInvalidTransition() {
        assertThrows(
            IllegalStateException.class,
            () -> SkillLifecycleState.DRAFT
                .transitionTo(
                    SkillLifecycleState.ACTIVE
                )
        );
    }

    @Test
    void shouldAllowActiveToDeprecated() {
        assertTrue(
            SkillLifecycleState.ACTIVE
                .canTransitionTo(
                    SkillLifecycleState.DEPRECATED
                )
        );
    }

    @Test
    void shouldAllowDeprecateToRetire() {
        assertTrue(
            SkillLifecycleState.DEPRECATED
                .canTransitionTo(
                    SkillLifecycleState.RETIRED
                )
        );
    }

    @Test
    void shouldAllowRevokeFromActive() {
        assertTrue(
            SkillLifecycleState.ACTIVE
                .canTransitionTo(
                    SkillLifecycleState.REVOKED
                )
        );
    }

    @Test
    void shouldAllowRevokeFromPublished() {
        assertTrue(
            SkillLifecycleState.PUBLISHED
                .canTransitionTo(
                    SkillLifecycleState.REVOKED
                )
        );
    }

    @Test
    void retiredShouldBeTerminal() {
        assertTrue(
            SkillLifecycleState.RETIRED
                .isTerminal()
        );
    }

    @Test
    void revokedShouldBeTerminal() {
        assertTrue(
            SkillLifecycleState.REVOKED
                .isTerminal()
        );
    }

    @Test
    void activeShouldBeExecutable() {
        assertTrue(
            SkillLifecycleState.ACTIVE
                .isExecutable()
        );
    }

    @Test
    void draftShouldNotBeExecutable() {
        assertFalse(
            SkillLifecycleState.DRAFT
                .isExecutable()
        );
    }

    @Test
    void publishedShouldBeImmutable() {
        assertTrue(
            SkillLifecycleState.PUBLISHED
                .isPublished()
        );
        assertTrue(
            SkillLifecycleState.ACTIVE
                .isPublished()
        );
        assertFalse(
            SkillLifecycleState.DRAFT
                .isPublished()
        );
    }

    @Test
    void retiredCannotTransition() {
        assertFalse(
            SkillLifecycleState.RETIRED
                .canTransitionTo(
                    SkillLifecycleState.ACTIVE
                )
        );
    }

    @Test
    void revokedCannotTransition() {
        assertFalse(
            SkillLifecycleState.REVOKED
                .canTransitionTo(
                    SkillLifecycleState.ACTIVE
                )
        );
    }

    // ── SkillDigest ──────────────────

    @Test
    void shouldComputeSha256Digest() {
        SkillDigest d = SkillDigest.sha256(
            "hello world"
        );
        assertEquals("SHA-256", d.algorithm());
        assertFalse(d.value().isBlank());
        assertEquals(64, d.value().length());
    }

    @Test
    void sameContentShouldProduceSameDigest() {
        SkillDigest d1 = SkillDigest.sha256(
            "test content"
        );
        SkillDigest d2 = SkillDigest.sha256(
            "test content"
        );
        assertEquals(d1, d2);
    }

    @Test
    void differentContentShouldDiffer() {
        SkillDigest d1 = SkillDigest.sha256(
            "content A"
        );
        SkillDigest d2 = SkillDigest.sha256(
            "content B"
        );
        assertNotEquals(d1, d2);
    }

    @Test
    void shouldVerifyContent() {
        SkillDigest d = SkillDigest.sha256(
            "verify me"
        );
        assertTrue(d.verify("verify me"));
        assertFalse(d.verify("tampered"));
    }

    @Test
    void shouldRejectBlankDigestValue() {
        assertThrows(
            IllegalArgumentException.class,
            () -> new SkillDigest(
                "SHA-256", ""
            )
        );
    }

    // ── SkillPackageMetadata ─────────

    @Test
    void shouldCreatePackageMetadata() {
        Schema in = () -> "object";
        Schema out = () -> "object";
        SkillContract c =
            SkillContract.of(in, out);

        SkillPackageMetadata m =
            new SkillPackageMetadata(
                new SkillId("test-skill"),
                new Version("1.0.0"),
                "Test Skill",
                "A test goal",
                c,
                SkillDependencies.empty(),
                SkillExecutionStrategy
                    .DIRECT_CAPABILITY,
                "team-a",
                "testing",
                Set.of("test", "demo"),
                Map.of("env", "dev"),
                URI.create(
                    "file:///tmp/test.jar"
                )
            );

        assertEquals(
            "test-skill",
            m.skillId().value()
        );
        assertEquals("team-a", m.owner());
        assertEquals(
            "testing", m.domain()
        );
        assertTrue(
            m.tags().contains("test")
        );
    }

    @Test
    void shouldConvertToDefinition() {
        Schema in = () -> "object";
        Schema out = () -> "object";
        SkillContract c =
            SkillContract.of(in, out);

        SkillPackageMetadata m =
            new SkillPackageMetadata(
                new SkillId("conv-skill"),
                new Version("2.0.0"),
                "Conv Skill",
                "Convert me",
                c,
                null,
                null,
                null, null, null, null, null
            );

        SkillDefinition def =
            m.toDefinition();

        assertEquals(
            "conv-skill",
            def.skillId().value()
        );
        assertEquals(
            SkillExecutionStrategy
                .DIRECT_CAPABILITY,
            def.executionStrategy()
        );
    }

    @Test
    void shouldRejectBlankName() {
        Schema s = () -> "object";
        SkillContract c =
            SkillContract.of(s, s);

        assertThrows(
            IllegalArgumentException.class,
            () -> new SkillPackageMetadata(
                new SkillId("x"),
                new Version("1.0.0"),
                "",
                "goal",
                c, null, null, null, null,
                null, null, null
            )
        );
    }

    // ── SkillQuery ───────────────────

    @Test
    void shouldCreateEmptyQuery() {
        SkillQuery q = SkillQuery.all();
        assertNull(q.idPattern());
        assertNull(q.owner());
        assertNull(q.lifecycleState());
    }

    @Test
    void shouldCreateByIdQuery() {
        SkillQuery q = SkillQuery.byId(
            new SkillId("my-skill")
        );
        assertEquals(
            "my-skill", q.idPattern()
        );
    }

    @Test
    void shouldBuildComplexQuery() {
        SkillQuery q = SkillQuery.builder()
            .idPattern("customer")
            .owner("team-a")
            .lifecycleState(
                SkillLifecycleState.ACTIVE
            )
            .tag("production")
            .build();

        assertEquals(
            "customer", q.idPattern()
        );
        assertEquals("team-a", q.owner());
        assertEquals(
            SkillLifecycleState.ACTIVE,
            q.lifecycleState()
        );
        assertEquals(
            "production", q.tag()
        );
    }

    // ── SkillRegistration ────────────

    @Test
    void shouldCreateRegistration() {
        Schema s = () -> "object";
        SkillContract c =
            SkillContract.of(s, s);

        Skill skill = new SkillDefinition(
            new SkillId("reg-skill"),
            new Version("1.0.0"),
            "Reg Skill",
            "Register me",
            c,
            SkillDependencies.empty(),
            SkillExecutionStrategy
                .DIRECT_CAPABILITY
        );

        SkillDescriptor desc =
            SkillDescriptor.from(skill);
        SkillDigest digest =
            SkillDigest.sha256("content");

        SkillRegistration reg =
            SkillRegistration.create(
                desc, digest
            );

        assertEquals(
            SkillLifecycleState.DRAFT,
            reg.state()
        );
        assertEquals(
            digest, reg.digest()
        );
        assertNotNull(
            reg.registeredAt()
        );
    }
}
