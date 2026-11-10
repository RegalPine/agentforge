package io.github.regalpine.agentforge.capability;

import io.github.regalpine.agentforge.skill.governance.SkillDigest;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * X4 Skill Runtime 增强测试。
 */
class SkillEnhancementTest {

    // --- TrustLevel ---

    @Test
    void shouldCheckTrustLevel() {
        assertTrue(
            SkillTrustLevel.ENTERPRISE
                .meetsMinimum(
                    SkillTrustLevel.APPLICATION
                )
        );
        assertFalse(
            SkillTrustLevel.LOCAL
                .meetsMinimum(
                    SkillTrustLevel.ENTERPRISE
                )
        );
        assertTrue(
            SkillTrustLevel.ENTERPRISE
                .canExecuteProduction()
        );
        assertFalse(
            SkillTrustLevel.LOCAL
                .canExecuteProduction()
        );
    }

    // --- LeastPrivilege ---

    @Test
    void shouldEnforceLeastPrivilege() {
        var lp = new SkillLeastPrivilege(
            Set.of("search", "calc"),
            Set.of("gpt-4"),
            Set.of("read"),
            1024, 2048, 30000
        );

        assertTrue(
            lp.isToolAllowed("search")
        );
        assertFalse(
            lp.isToolAllowed("delete")
        );
        assertTrue(
            lp.isModelAllowed("gpt-4")
        );
        assertFalse(
            lp.isModelAllowed("gpt-3")
        );
        assertTrue(
            lp.isActionAllowed("read")
        );
        assertFalse(
            lp.isActionAllowed("write")
        );
        assertTrue(
            lp.isInputWithinLimit(512)
        );
        assertFalse(
            lp.isInputWithinLimit(2048)
        );
    }

    @Test
    void shouldUnrestrictedAllowAll() {
        var lp =
            SkillLeastPrivilege
                .unrestricted();
        assertTrue(
            lp.isToolAllowed("anything")
        );
        assertTrue(
            lp.isActionAllowed("anything")
        );
    }

    @Test
    void shouldValidateViolations() {
        var lp = new SkillLeastPrivilege(
            Set.of("search"),
            Set.of(),
            Set.of("read"),
            0, 0, 0
        );

        var violations = lp.validate(
            "delete", "write"
        );
        assertEquals(2,
            violations.size()
        );
    }

    // --- VersionConstraint ---

    @Test
    void shouldParseVersion() {
        var v =
            SkillVersionConstraint
                .parse("1.2.3");
        assertEquals(1, v.major());
        assertEquals(2, v.minor());
        assertEquals(3, v.patch());
    }

    @Test
    void shouldCompareVersions() {
        var v1 =
            SkillVersionConstraint
                .parse("1.0.0");
        var v2 =
            SkillVersionConstraint
                .parse("1.1.0");
        var v3 =
            SkillVersionConstraint
                .parse("2.0.0");

        assertTrue(
            v2.compareTo(v1) > 0
        );
        assertTrue(
            v1.compareTo(v2) < 0
        );
        assertTrue(
            v2.satisfies(v1)
        );
        assertFalse(
            v1.satisfies(v2)
        );
    }

    @Test
    void shouldCheckCompatibility() {
        var v1 =
            SkillVersionConstraint
                .parse("1.0.0");
        var v2 =
            SkillVersionConstraint
                .parse("1.5.0");
        var v3 =
            SkillVersionConstraint
                .parse("2.0.0");

        assertTrue(
            v1.isCompatibleWith(v2)
        );
        assertFalse(
            v1.isCompatibleWith(v3)
        );
    }

    @Test
    void shouldRejectInvalidVersion() {
        assertThrows(
            IllegalArgumentException.class,
            () -> SkillVersionConstraint
                .parse("1.0")
        );
        assertThrows(
            IllegalArgumentException.class,
            () -> SkillVersionConstraint
                .parse("a.b.c")
        );
    }

    // --- TenantIsolation ---

    @Test
    void shouldIsolateTenants() {
        var isolation =
            new SkillTenantIsolation();

        isolation.register(
            "tenant-a", "skill-1"
        );
        isolation.register(
            "tenant-a", "skill-2"
        );
        isolation.register(
            "tenant-b", "skill-1"
        );

        assertTrue(
            isolation.isAccessible(
                "tenant-a", "skill-1"
            )
        );
        assertTrue(
            isolation.isAccessible(
                "tenant-a", "skill-2"
            )
        );
        assertFalse(
            isolation.isAccessible(
                "tenant-b", "skill-2"
            )
        );
        assertEquals(
            2,
            isolation.visibleSkills(
                "tenant-a"
            ).size()
        );
        assertEquals(
            1,
            isolation.visibleSkills(
                "tenant-b"
            ).size()
        );
    }

    @Test
    void shouldRemoveTenant() {
        var isolation =
            new SkillTenantIsolation();
        isolation.register(
            "t1", "s1"
        );
        isolation.removeTenant("t1");
        assertTrue(
            isolation.visibleSkills("t1")
                .isEmpty()
        );
    }

    // --- CacheManager ---

    @Test
    void shouldCacheAndGet() {
        var cache =
            new SkillCacheManager();
        cache.put("key1", "value1");
        assertEquals(
            "value1",
            cache.<String>get("key1")
        );
        assertTrue(
            cache.containsKey("key1")
        );
    }

    @Test
    void shouldInvalidate() {
        var cache =
            new SkillCacheManager();
        cache.put("key1", "value1");
        cache.invalidate("key1");
        assertNull(
            cache.get("key1")
        );
    }

    @Test
    void shouldInvalidateAll() {
        var cache =
            new SkillCacheManager();
        cache.put("k1", "v1");
        cache.put("k2", "v2");
        cache.invalidateAll();
        assertEquals(0,
            cache.size()
        );
    }

    // --- SignatureVerifier ---

    @Test
    void shouldVerifyDigest() {
        var verifier =
            new SkillSignatureVerifier();
        String content = "test content";
        SkillDigest digest =
            SkillDigest.sha256(content);

        var result = verifier.verify(
            content.getBytes(),
            digest
        );
        assertTrue(result.valid());
    }

    @Test
    void shouldRejectTampered() {
        var verifier =
            new SkillSignatureVerifier();
        String content = "original";
        SkillDigest digest =
            SkillDigest.sha256(content);

        var result = verifier.verify(
            "tampered".getBytes(),
            digest
        );
        assertFalse(result.valid());
    }

    @Test
    void shouldThrowOnTampered() {
        var verifier =
            new SkillSignatureVerifier();
        String content = "original";
        SkillDigest digest =
            SkillDigest.sha256(content);

        assertThrows(
            SkillSignatureVerifier
                .SkillIntegrityException
                .class,
            () -> verifier.verifyOrThrow(
                "tampered".getBytes(),
                digest
            )
        );
    }
}
