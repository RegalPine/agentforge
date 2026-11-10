package io.github.regalpine.agentforge.skill.governance;

import io.github.regalpine.agentforge.kernel.EntityId;
import io.github.regalpine.agentforge.kernel.CapabilityRef;
import io.github.regalpine.agentforge.kernel.Reference;
import io.github.regalpine.agentforge.kernel.Schema;
import io.github.regalpine.agentforge.kernel.Skill;
import io.github.regalpine.agentforge.kernel.SkillContract;
import io.github.regalpine.agentforge.kernel.SkillDefinition;
import io.github.regalpine.agentforge.kernel.SkillDependencies;
import io.github.regalpine.agentforge.kernel.SkillDescriptor;
import io.github.regalpine.agentforge.kernel.SkillExecutionStrategy;
import io.github.regalpine.agentforge.kernel.SkillId;
import io.github.regalpine.agentforge.kernel.SkillReference;
import io.github.regalpine.agentforge.kernel.Version;


import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Skill 语义模型测试.
 * T-S0.15: 不可变性 / 版本比较 / 引用构造 /
 * 依赖声明 / Strategy 枚举 / 错误码.
 */
class SkillSemanticModelTest {

    // ── SkillId ──────────────────────────

    @Test
    void skillIdShouldAcceptValidChars() {
        SkillId id = new SkillId(
            "customer-analysis.v1"
        );
        assertEquals(
            "customer-analysis.v1",
            id.value()
        );
    }

    @Test
    void skillIdShouldRejectBlank() {
        assertThrows(
            IllegalArgumentException.class,
            () -> new SkillId("")
        );
    }

    @Test
    void skillIdShouldRejectNull() {
        assertThrows(
            NullPointerException.class,
            () -> new SkillId(null)
        );
    }

    @Test
    void skillIdShouldRejectInvalidChars() {
        assertThrows(
            IllegalArgumentException.class,
            () -> new SkillId("invalid id")
        );
    }

    @Test
    void skillIdShouldConvertToEntityId() {
        SkillId sid = new SkillId("test.skill");
        EntityId eid = sid.toEntityId();
        assertEquals(
            "test.skill",
            eid.value()
        );
    }

    @Test
    void skillIdShouldSupportEquality() {
        SkillId a = new SkillId("test");
        SkillId b = new SkillId("test");
        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
    }

    // ── Version comparison ───────────────

    @Test
    void versionShouldCompareCorrectly() {
        Version v1 = new Version("1.0.0");
        Version v2 = new Version("1.1.0");
        Version v3 = new Version("2.0.0");

        assertTrue(v1.compareTo(v2) < 0);
        assertTrue(v2.compareTo(v1) > 0);
        assertTrue(v3.compareTo(v2) > 0);
        assertEquals(0, v1.compareTo(
            new Version("1.0.0")
        ));
    }

    @Test
    void versionShouldParseComponents() {
        Version v = new Version("3.14.2");
        assertEquals(3, v.major());
        assertEquals(14, v.minor());
        assertEquals(2, v.patch());
    }

    @Test
    void versionShouldCheckCompatibility() {
        Version v1 = new Version("1.0.0");
        Version v2 = new Version("1.5.3");
        Version v3 = new Version("2.0.0");

        assertTrue(
            v1.isCompatibleWith(v2)
        );
        assertFalse(
            v1.isCompatibleWith(v3)
        );
    }

    @Test
    void versionShouldCheckNewer() {
        Version v1 = new Version("1.0.0");
        Version v2 = new Version("1.1.0");

        assertTrue(v2.isNewerThan(v1));
        assertFalse(v1.isNewerThan(v2));
    }

    // ── SkillReference ───────────────────

    @Test
    void skillReferenceShouldCreate() {
        SkillReference ref = SkillReference.of(
            "my-skill", "1.0.0"
        );
        assertEquals(
            "my-skill",
            ref.skillId().value()
        );
        assertEquals(
            "1.0.0",
            ref.version().value()
        );
    }

    @Test
    void skillReferenceShouldConvertToReference() {
        SkillReference sr = SkillReference.of(
            "test", "2.0.0"
        );
        Reference r = sr.toReference();
        assertEquals(
            "test",
            r.id().value()
        );
        assertEquals(
            "2.0.0",
            r.version().value()
        );
    }

    @Test
    void skillReferenceShouldRejectNull() {
        assertThrows(
            NullPointerException.class,
            () -> new SkillReference(null, null)
        );
    }

    // ── SkillContract ────────────────────

    @Test
    void skillContractShouldCreate() {
        Schema in = () -> "object";
        Schema out = () -> "object";
        SkillContract c = SkillContract.of(
            in, out
        );
        assertEquals("object", c.inputSchema().type());
        assertEquals("object", c.outputSchema().type());
    }

    @Test
    void skillContractShouldRejectNull() {
        assertThrows(
            NullPointerException.class,
            () -> SkillContract.of(null, null)
        );
    }

    // ── SkillDependencies ────────────────

    @Test
    void skillDependenciesShouldBeEmpty() {
        SkillDependencies d =
            SkillDependencies.empty();
        assertTrue(d.isEmpty());
        assertTrue(d.capabilities().isEmpty());
        assertTrue(d.knowledge().isEmpty());
        assertTrue(d.skills().isEmpty());
    }

    @Test
    void skillDependenciesShouldHoldRefs() {
        SkillDependencies d = SkillDependencies.of(
            Set.of(CapabilityRef.of("cap", "1.0.0")),
            Set.of(Reference.of("know", "1.0.0")),
            Set.of(SkillReference.of("sk", "1.0.0"))
        );
        assertFalse(d.isEmpty());
        assertEquals(1, d.capabilities().size());
        assertEquals(1, d.knowledge().size());
        assertEquals(1, d.skills().size());
    }

    @Test
    void skillDependenciesShouldDefendCopy() {
        SkillDependencies d =
            SkillDependencies.of(null, null, null);
        assertTrue(d.isEmpty());
    }

    // ── SkillExecutionStrategy ───────────

    @Test
    void skillExecutionStrategyShouldHaveFourValues() {
        SkillExecutionStrategy[] values =
            SkillExecutionStrategy.values();
        assertEquals(4, values.length);
        assertNotNull(
            SkillExecutionStrategy.DIRECT_CAPABILITY
        );
        assertNotNull(
            SkillExecutionStrategy.GRAPH
        );
        assertNotNull(
            SkillExecutionStrategy.AGENT
        );
        assertNotNull(
            SkillExecutionStrategy.COMPOSITE_SKILL
        );
    }

    // ── SkillErrorCode ───────────────────

    @Test
    void skillErrorCodeShouldHaveTwelveValues() {
        SkillErrorCode[] values =
            SkillErrorCode.values();
        assertEquals(12, values.length);
    }

    // ── SkillDefinition ──────────────────

    @Test
    void skillDefinitionShouldImplementSkill() {
        Schema in = () -> "object";
        Schema out = () -> "object";
        SkillContract contract =
            SkillContract.of(in, out);

        SkillDefinition skill =
            SkillDefinition.of(
                "test-skill",
                "1.0.0",
                "Test Skill",
                "A test skill",
                contract
            );

        assertEquals(
            "test-skill",
            skill.skillId().value()
        );
        assertEquals(
            "test-skill",
            skill.id().value()
        );
        assertEquals(
            "1.0.0",
            skill.version().value()
        );
        assertEquals(
            "Test Skill",
            skill.name()
        );
        assertEquals(
            "A test skill",
            skill.goal()
        );
        assertEquals(
            SkillExecutionStrategy.DIRECT_CAPABILITY,
            skill.executionStrategy()
        );
        assertTrue(
            skill.dependencies().isEmpty()
        );
    }

    @Test
    void skillDefinitionShouldRejectBlankName() {
        Schema s = () -> "object";
        assertThrows(
            IllegalArgumentException.class,
            () -> SkillDefinition.of(
                "id", "1.0.0", "",
                "goal",
                SkillContract.of(s, s)
            )
        );
    }

    @Test
    void skillDefinitionShouldDefaultStrategy() {
        Schema s = () -> "object";
        SkillDefinition skill =
            new SkillDefinition(
                new SkillId("test"),
                new Version("1.0.0"),
                "Test",
                "Goal",
                SkillContract.of(s, s),
                null,
                null
            );
        assertEquals(
            SkillExecutionStrategy.DIRECT_CAPABILITY,
            skill.executionStrategy()
        );
        assertTrue(
            skill.dependencies().isEmpty()
        );
    }

    // ── SkillDescriptor ──────────────────

    @Test
    void skillDescriptorShouldCreateFromSkill() {
        Schema s = () -> "object";
        Skill skill = SkillDefinition.of(
            "my-skill", "2.0.0",
            "My Skill", "Does things",
            SkillContract.of(s, s)
        );

        SkillDescriptor desc =
            SkillDescriptor.from(skill);

        assertEquals(
            "my-skill",
            desc.skillId().value()
        );
        assertEquals("My Skill", desc.name());
        assertEquals("2.0.0", desc.version().value());
        assertEquals("DRAFT", desc.status());
    }

    @Test
    void skillDescriptorShouldRejectBlankName() {
        assertThrows(
            IllegalArgumentException.class,
            () -> new SkillDescriptor(
                new SkillId("id"),
                "",
                new Version("1.0.0"),
                "goal",
                SkillExecutionStrategy.GRAPH,
                "",
                "DRAFT"
            )
        );
    }
}
