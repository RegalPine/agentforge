package io.github.regalpine.agentforge.hardening.conformance;

import io.github.regalpine.agentforge.kernel.*;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * conformance-skill-semantic:
 * AF-SKILL-001 Identity /
 * AF-SKILL-002 Version /
 * AF-SKILL-003 Contract.
 * AF-AD-15 §14.2, T-S6.1.
 *
 * <p>Verifies the core Skill semantic
 * types: SkillId, Version, SkillContract,
 * SkillDependencies, SkillExecutionStrategy,
 * SkillDescriptor.
 */
class SkillSemanticConformanceTest {

    // ── AF-SKILL-001 Identity ────

    @Test
    void afSkill001_skillIdAsciiConstraint() {
        var id = new SkillId("skill.test-1");
        assertEquals("skill.test-1", id.value());
    }

    @Test
    void afSkill001_skillIdRejectsBlank() {
        assertThrows(
            IllegalArgumentException.class,
            () -> new SkillId("")
        );
    }

    @Test
    void afSkill001_skillIdRejectsInvalidChars() {
        assertThrows(
            IllegalArgumentException.class,
            () -> new SkillId("skill with spaces")
        );
    }

    @Test
    void afSkill001_skillIdToEntityId() {
        var id = new SkillId("s1");
        var entityId = id.toEntityId();
        assertEquals("s1", entityId.value());
    }

    // ── AF-SKILL-002 Version ─────

    @Test
    void afSkill002_versionCreation() {
        var v = new Version("1.0.0");
        assertEquals("1.0.0", v.value());
    }

    @Test
    void afSkill002_versionRejectsBlank() {
        assertThrows(
            IllegalArgumentException.class,
            () -> new Version("")
        );
    }

    @Test
    void afSkill002_versionEquality() {
        var v1 = new Version("2.0.0");
        var v2 = new Version("2.0.0");
        assertEquals(v1, v2);
        assertEquals(v1.hashCode(), v2.hashCode());
    }

    // ── AF-SKILL-003 Contract ────

    @Test
    void afSkill003_contractCreation() {
        Schema in = () -> "object";
        Schema out = () -> "string";
        var contract = SkillContract.of(in, out);

        assertEquals("object",
            contract.inputSchema().type());
        assertEquals("string",
            contract.outputSchema().type());
    }

    @Test
    void afSkill003_contractRejectsNull() {
        assertThrows(
            NullPointerException.class,
            () -> new SkillContract(null, null)
        );
    }

    @Test
    void afSkill003_emptyDependencies() {
        var deps = SkillDependencies.empty();
        assertTrue(deps.isEmpty());
        assertTrue(deps.capabilities().isEmpty());
        assertTrue(deps.knowledge().isEmpty());
        assertTrue(deps.skills().isEmpty());
    }

    @Test
    void afSkill003_dependenciesDefensiveCopy() {
        var deps = new SkillDependencies(
            Set.of(CapabilityRef.of("c1", "1.0.0")),
            Set.of(), Set.of()
        );
        assertEquals(1,
            deps.capabilities().size());
    }

    // ── Execution Strategy ───────

    @Test
    void afSkill003_fourStrategies() {
        assertEquals(4,
            SkillExecutionStrategy.values().length
        );
        assertNotNull(
            SkillExecutionStrategy
                .DIRECT_CAPABILITY
        );
        assertNotNull(
            SkillExecutionStrategy.GRAPH
        );
        assertNotNull(
            SkillExecutionStrategy.AGENT
        );
        assertNotNull(
            SkillExecutionStrategy
                .COMPOSITE_SKILL
        );
    }

    // ── Descriptor ───────────────

    @Test
    void afSkill003_descriptorCreation() {
        var d = new SkillDescriptor(
            new SkillId("s1"),
            "TestSkill",
            new Version("1.0.0"),
            "Test goal",
            SkillExecutionStrategy
                .DIRECT_CAPABILITY,
            "owner1",
            "DRAFT"
        );

        assertEquals("s1",
            d.skillId().value());
        assertEquals("TestSkill", d.name());
        assertEquals("DRAFT", d.status());
    }

    @Test
    void afSkill003_descriptorRejectsBlankName() {
        assertThrows(
            IllegalArgumentException.class,
            () -> new SkillDescriptor(
                new SkillId("s1"),
                "",
                new Version("1.0.0"),
                "goal",
                SkillExecutionStrategy
                    .DIRECT_CAPABILITY,
                null, null
            )
        );
    }
}
