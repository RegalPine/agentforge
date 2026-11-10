package io.github.regalpine.agentforge.hardening.conformance;

import io.github.regalpine.agentforge.capability.InMemorySkillRegistry;
import io.github.regalpine.agentforge.kernel.*;
import io.github.regalpine.agentforge.skill.governance.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * conformance-skill-registry:
 * AF-SKILL-REG-001 ~ 020.
 * Register / Resolve / Discover /
 * Version / Lifecycle / Digest /
 * Dependency / Tenant / Package /
 * Manifest.
 * AF-AD-15 §14.2, T-S6.2~T-S6.3.
 */
class SkillRegistryConformanceTest {

    private InMemorySkillRegistry registry;
    private SkillPackageMetadata metadata;
    private SkillDigest digest;

    @BeforeEach
    void setUp() {
        registry = new InMemorySkillRegistry();
        Schema s = () -> "object";
        metadata = new SkillPackageMetadata(
            new SkillId("reg-test"),
            new Version("1.0.0"),
            "RegTest Skill",
            "Test goal",
            SkillContract.of(s, s),
            SkillDependencies.empty(),
            SkillExecutionStrategy
                .DIRECT_CAPABILITY,
            "owner1", "domain1",
            Set.of("tag1"),
            null,
            URI.create("file:///test.tar.gz")
        );
        digest = SkillDigest.sha256("test");
    }

    // ── AF-SKILL-REG-001 Register ─

    @Test
    void afSkillReg001_registerReturnsDraft() {
        var reg = registry.register(
            metadata, digest
        );
        assertEquals(
            SkillLifecycleState.DRAFT,
            reg.state()
        );
        assertNotNull(reg.registeredAt());
    }

    // ── AF-SKILL-REG-002 Duplicate ─

    @Test
    void afSkillReg002_duplicateThrows() {
        registry.register(metadata, digest);
        assertThrows(
            IllegalArgumentException.class,
            () -> registry.register(
                metadata, digest
            )
        );
    }

    // ── AF-SKILL-REG-003 Resolve ───

    @Test
    void afSkillReg003_resolveFound() {
        registry.register(metadata, digest);
        var ref = new SkillReference(
            new SkillId("reg-test"),
            new Version("1.0.0")
        );
        var resolved = registry.resolve(ref);
        assertTrue(resolved.isPresent());
        assertEquals("RegTest Skill",
            resolved.get().name());
    }

    @Test
    void afSkillReg003_resolveMissing() {
        var ref = new SkillReference(
            new SkillId("nonexistent"),
            new Version("1.0.0")
        );
        assertTrue(
            registry.resolve(ref).isEmpty()
        );
    }

    // ── AF-SKILL-REG-004 Discover ──

    @Test
    void afSkillReg004_discoverAll() {
        registry.register(metadata, digest);
        var all = registry.list();
        assertEquals(1, all.size());
    }

    @Test
    void afSkillReg004_discoverByQuery() {
        registry.register(metadata, digest);
        var q = new SkillQuery(
            "reg-test", null,
            null, null, null, null, null
        );
        var found = registry.discover(q);
        assertEquals(1, found.size());
    }

    @Test
    void afSkillReg004_discoverByOwner() {
        registry.register(metadata, digest);
        var q = new SkillQuery(
            null, null,
            "owner1", null, null, null, null
        );
        var found = registry.discover(q);
        assertEquals(1, found.size());
    }

    // ── AF-SKILL-REG-005 Version ───

    @Test
    void afSkillReg005_multipleVersions() {
        registry.register(metadata, digest);

        Schema s = () -> "object";
        var v2 = new SkillPackageMetadata(
            new SkillId("reg-test"),
            new Version("2.0.0"),
            "RegTest Skill V2",
            "Test goal v2",
            SkillContract.of(s, s),
            SkillDependencies.empty(),
            SkillExecutionStrategy
                .DIRECT_CAPABILITY,
            "owner1", "domain1",
            null, null,
            URI.create("file:///v2.tar.gz")
        );
        var d2 = SkillDigest.sha256("v2");
        registry.register(v2, d2);

        assertEquals(2, registry.list().size());
    }

    // ── AF-SKILL-REG-006 Lifecycle ─

    @Test
    void afSkillReg006_lifecycleTransition() {
        registry.register(metadata, digest);
        var ref = new SkillReference(
            new SkillId("reg-test"),
            new Version("1.0.0")
        );

        assertEquals(
            SkillLifecycleState.DRAFT,
            registry.state(ref)
        );

        registry.transition(
            ref, SkillLifecycleState.VALIDATED
        );
        assertEquals(
            SkillLifecycleState.VALIDATED,
            registry.state(ref)
        );
    }

    @Test
    void afSkillReg006_lifecycleMissingThrows() {
        var ref = new SkillReference(
            new SkillId("missing"),
            new Version("1.0.0")
        );
        assertThrows(
            IllegalArgumentException.class,
            () -> registry.transition(
                ref, SkillLifecycleState.ACTIVE
            )
        );
    }

    // ── AF-SKILL-REG-007 Digest ────

    @Test
    void afSkillReg007_digestRetrieve() {
        registry.register(metadata, digest);
        var ref = new SkillReference(
            new SkillId("reg-test"),
            new Version("1.0.0")
        );
        var d = registry.digest(ref);
        assertTrue(d.isPresent());
        assertEquals(
            digest.value(), d.get().value()
        );
    }

    // ── AF-SKILL-REG-008 Metadata ──

    @Test
    void afSkillReg008_metadataRetrieve() {
        registry.register(metadata, digest);
        var ref = new SkillReference(
            new SkillId("reg-test"),
            new Version("1.0.0")
        );
        var m = registry.metadata(ref);
        assertTrue(m.isPresent());
        assertEquals("owner1", m.get().owner());
    }

    // ── AF-SKILL-REG-009 Unregister ─

    @Test
    void afSkillReg009_unregister() {
        registry.register(metadata, digest);
        var ref = new SkillReference(
            new SkillId("reg-test"),
            new Version("1.0.0")
        );
        registry.unregister(ref);
        assertTrue(
            registry.resolve(ref).isEmpty()
        );
    }

    // ── AF-SKILL-REG-010 Package ────

    @Test
    void afSkillReg010_packageMetadata() {
        assertEquals("RegTest Skill",
            metadata.name());
        assertEquals("Test goal",
            metadata.goal());
        assertEquals("domain1",
            metadata.domain());
        assertTrue(
            metadata.tags().contains("tag1")
        );
    }

    @Test
    void afSkillReg010_toDescriptor() {
        var desc = metadata.toDescriptor(
            "DRAFT"
        );
        assertEquals("reg-test",
            desc.skillId().value());
        assertEquals("DRAFT", desc.status());
    }

    @Test
    void afSkillReg010_toDefinition() {
        var def = metadata.toDefinition();
        assertEquals("reg-test",
            def.skillId().value());
        assertEquals("RegTest Skill",
            def.name());
    }
}
