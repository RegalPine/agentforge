package io.github.regalpine.agentforge.capability;

import io.github.regalpine.agentforge.kernel.*;
import io.github.regalpine.agentforge.skill.governance.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * InMemorySkillRegistry 测试.
 * T-S1.16: 注册 / 解析 / 发现 /
 * 生命周期迁移 / 版本不可变 / Digest /
 * Tenant 隔离.
 */
class InMemorySkillRegistryTest {

    private InMemorySkillRegistry registry;

    @BeforeEach
    void setUp() {
        registry =
            new InMemorySkillRegistry();
    }

    // ── Register & Resolve ───────────

    @Nested
    class RegisterAndResolve {

        @Test
        void shouldRegisterAndResolve() {
            SkillPackageMetadata m =
                makeMetadata(
                    "skill-a", "1.0.0"
                );
            SkillDigest d =
                SkillDigest.sha256("a");

            SkillRegistration reg =
                registry.register(m, d);

            assertEquals(
                SkillLifecycleState.DRAFT,
                reg.state()
            );

            SkillReference ref =
                SkillReference.of(
                    "skill-a", "1.0.0"
                );

            Optional<SkillDescriptor> found =
                registry.resolve(ref);

            assertTrue(found.isPresent());
            assertEquals(
                "skill-a",
                found.get().skillId().value()
            );
        }

        @Test
        void shouldRejectDuplicate() {
            SkillPackageMetadata m =
                makeMetadata(
                    "dup", "1.0.0"
                );
            SkillDigest d =
                SkillDigest.sha256("dup");

            registry.register(m, d);

            assertThrows(
                IllegalArgumentException.class,
                () -> registry.register(m, d)
            );
        }

        @Test
        void shouldReturnEmptyForUnknown() {
            SkillReference ref =
                SkillReference.of(
                    "unknown", "1.0.0"
                );
            assertTrue(
                registry.resolve(ref).isEmpty()
            );
        }

        @Test
        void shouldReturnMetadata() {
            SkillPackageMetadata m =
                makeMetadata(
                    "meta-skill", "1.0.0"
                );
            registry.register(
                m, SkillDigest.sha256("m")
            );

            SkillReference ref =
                SkillReference.of(
                    "meta-skill", "1.0.0"
                );

            Optional<SkillPackageMetadata> found =
                registry.metadata(ref);

            assertTrue(found.isPresent());
            assertEquals(
                "meta-skill",
                found.get().skillId().value()
            );
        }

        @Test
        void shouldReturnDigest() {
            SkillDigest d =
                SkillDigest.sha256("content");
            SkillPackageMetadata m =
                makeMetadata(
                    "dig-skill", "1.0.0"
                );
            registry.register(m, d);

            SkillReference ref =
                SkillReference.of(
                    "dig-skill", "1.0.0"
                );

            Optional<SkillDigest> found =
                registry.digest(ref);

            assertTrue(found.isPresent());
            assertEquals(d, found.get());
        }
    }

    // ── Discover ─────────────────────

    @Nested
    class Discover {

        @Test
        void shouldDiscoverAll() {
            registry.register(
                makeMetadata("s1", "1.0.0"),
                SkillDigest.sha256("1")
            );
            registry.register(
                makeMetadata("s2", "1.0.0"),
                SkillDigest.sha256("2")
            );

            List<SkillDescriptor> all =
                registry.discover(
                    SkillQuery.all()
                );

            assertEquals(2, all.size());
        }

        @Test
        void shouldDiscoverByIdPattern() {
            registry.register(
                makeMetadata(
                    "customer-analysis",
                    "1.0.0"
                ),
                SkillDigest.sha256("ca")
            );
            registry.register(
                makeMetadata(
                    "order-summary",
                    "1.0.0"
                ),
                SkillDigest.sha256("os")
            );

            List<SkillDescriptor> found =
                registry.discover(
                    SkillQuery.builder()
                        .idPattern("customer")
                        .build()
                );

            assertEquals(1, found.size());
            assertEquals(
                "customer-analysis",
                found.get(0).skillId().value()
            );
        }

        @Test
        void shouldDiscoverByOwner() {
            SkillPackageMetadata m1 =
                new SkillPackageMetadata(
                    new SkillId("owned-1"),
                    new Version("1.0.0"),
                    "Owned 1", "goal",
                    makeContract(),
                    null, null,
                    "team-a", null,
                    null, null, null
                );
            SkillPackageMetadata m2 =
                new SkillPackageMetadata(
                    new SkillId("owned-2"),
                    new Version("1.0.0"),
                    "Owned 2", "goal",
                    makeContract(),
                    null, null,
                    "team-b", null,
                    null, null, null
                );

            registry.register(
                m1, SkillDigest.sha256("o1")
            );
            registry.register(
                m2, SkillDigest.sha256("o2")
            );

            List<SkillDescriptor> found =
                registry.discover(
                    SkillQuery.byOwner("team-a")
                );

            assertEquals(1, found.size());
            assertEquals(
                "owned-1",
                found.get(0).skillId().value()
            );
        }

        @Test
        void shouldListAll() {
            registry.register(
                makeMetadata("a", "1.0.0"),
                SkillDigest.sha256("a")
            );
            registry.register(
                makeMetadata("b", "2.0.0"),
                SkillDigest.sha256("b")
            );

            assertEquals(
                2, registry.list().size()
            );
        }
    }

    // ── Lifecycle ────────────────────

    @Nested
    class Lifecycle {

        @Test
        void shouldTransitionDraftToValidated() {
            registerDefault("lc-skill");

            SkillReference ref =
                SkillReference.of(
                    "lc-skill", "1.0.0"
                );

            registry.transition(
                ref,
                SkillLifecycleState.VALIDATED
            );

            assertEquals(
                SkillLifecycleState.VALIDATED,
                registry.state(ref)
            );
        }

        @Test
        void shouldRejectDraftToActive() {
            registerDefault("lc-skill");

            SkillReference ref =
                SkillReference.of(
                    "lc-skill", "1.0.0"
                );

            assertThrows(
                IllegalStateException.class,
                () -> registry.transition(
                    ref,
                    SkillLifecycleState.ACTIVE
                )
            );
        }

        @Test
        void shouldDeprecate() {
            registerAndAdvance("dep-skill");

            SkillReference ref =
                SkillReference.of(
                    "dep-skill", "1.0.0"
                );

            registry.deprecate(ref);

            assertEquals(
                SkillLifecycleState.DEPRECATED,
                registry.state(ref)
            );
        }

        @Test
        void shouldRetire() {
            registerAndAdvance("ret-skill");
            SkillReference ref =
                SkillReference.of(
                    "ret-skill", "1.0.0"
                );

            registry.deprecate(ref);
            registry.retire(ref);

            assertEquals(
                SkillLifecycleState.RETIRED,
                registry.state(ref)
            );
        }

        @Test
        void shouldRevoke() {
            registerAndAdvance("rev-skill");
            SkillReference ref =
                SkillReference.of(
                    "rev-skill", "1.0.0"
                );

            registry.revoke(ref);

            assertEquals(
                SkillLifecycleState.REVOKED,
                registry.state(ref)
            );
        }

        @Test
        void publishedVersionCannotChange() {
            // advance to PUBLISHED
            registerDefault("pub-skill");
            SkillReference ref =
                SkillReference.of(
                    "pub-skill", "1.0.0"
                );

            advanceTo(ref,
                SkillLifecycleState.VALIDATED);
            advanceTo(ref,
                SkillLifecycleState.TESTED);
            advanceTo(ref,
                SkillLifecycleState.EVALUATED);
            advanceTo(ref,
                SkillLifecycleState.APPROVED);
            advanceTo(ref,
                SkillLifecycleState.PUBLISHED);

            assertEquals(
                SkillLifecycleState.PUBLISHED,
                registry.state(ref)
            );

            // PUBLISHED → DRAFT not allowed
            assertThrows(
                IllegalStateException.class,
                () -> registry.transition(
                    ref,
                    SkillLifecycleState.DRAFT
                )
            );
        }
    }

    // ── Unregister ───────────────────

    @Test
    void shouldUnregister() {
        registerDefault("unreg-skill");
        SkillReference ref =
            SkillReference.of(
                "unreg-skill", "1.0.0"
            );

        registry.unregister(ref);

        assertTrue(
            registry.resolve(ref).isEmpty()
        );
    }

    // ── helpers ──────────────────────

    private static SkillContract
        makeContract()
    {
        Schema s = () -> "object";
        return SkillContract.of(s, s);
    }

    private static SkillPackageMetadata
        makeMetadata(
            String id, String version
        )
    {
        return new SkillPackageMetadata(
            new SkillId(id),
            new Version(version),
            id,
            "goal for " + id,
            makeContract(),
            SkillDependencies.empty(),
            SkillExecutionStrategy
                .DIRECT_CAPABILITY,
            null, null, null, null,
            URI.create(
                "file:///tmp/" + id + ".jar"
            )
        );
    }

    private void registerDefault(
        String id
    ) {
        registry.register(
            makeMetadata(id, "1.0.0"),
            SkillDigest.sha256(id)
        );
    }

    private void registerAndAdvance(
        String id
    ) {
        registerDefault(id);
        SkillReference ref =
            SkillReference.of(id, "1.0.0");
        advanceTo(ref,
            SkillLifecycleState.ACTIVE);
    }

    private void advanceTo(
        SkillReference ref,
        SkillLifecycleState target
    ) {
        SkillLifecycleState current =
            registry.state(ref);

        if (current == target) return;

        // find next step in lifecycle
        SkillLifecycleState[] steps = {
            SkillLifecycleState.VALIDATED,
            SkillLifecycleState.TESTED,
            SkillLifecycleState.EVALUATED,
            SkillLifecycleState.APPROVED,
            SkillLifecycleState.PUBLISHED,
            SkillLifecycleState.ACTIVE,
            SkillLifecycleState.DEPRECATED,
            SkillLifecycleState.RETIRED
        };

        for (SkillLifecycleState step : steps) {
            if (current.canTransitionTo(step)
                && (step == target
                    || isBefore(step, target)))
            {
                registry.transition(ref, step);
                current = step;
                if (current == target) return;
            }
        }
    }

    private static boolean isBefore(
        SkillLifecycleState a,
        SkillLifecycleState b
    ) {
        return a.ordinal() < b.ordinal();
    }
}
