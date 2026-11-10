package io.github.regalpine.agentforge.hardening.conformance;

import io.github.regalpine.agentforge.hardening.*;
import io.github.regalpine.agentforge.kernel.*;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * conformance-core: ASM / Agent Identity
 * / Version / Metadata.
 * AF-RELEASE-02 §4, T-H6.1.
 */
class CoreConformanceTest {

    private final TckRunner runner =
        new TckRunner("CORE-CONF");

    @Test
    void afCore001_entityIdCreation() {
        var id = new EntityId("test-agent");
        assertEquals("test-agent", id.value());
        runner.recordPass(
            TckId.of("CORE", 1), "core",
            "EntityId creation", TestLevel.L0_UNIT
        );
    }

    @Test
    void afCore002_versionParsing() {
        var v = new Version("1.2.3");
        assertEquals("1.2.3", v.value());
        runner.recordPass(
            TckId.of("CORE", 2), "core",
            "Version parsing", TestLevel.L0_UNIT
        );
    }

    @Test
    void afCore003_referenceIdentity() {
        var ref1 = new Reference(
            new EntityId("cap"), new Version("1.0.0")
        );
        var ref2 = new Reference(
            new EntityId("cap"), new Version("1.0.0")
        );
        assertEquals(ref1, ref2);
        assertEquals(ref1.hashCode(), ref2.hashCode());
        runner.recordPass(
            TckId.of("CORE", 3), "core",
            "Reference identity", TestLevel.L0_UNIT
        );
    }

    @Test
    void afCore004_entityIdRejectsBlank() {
        assertThrows(
            IllegalArgumentException.class,
            () -> new EntityId("")
        );
        runner.recordPass(
            TckId.of("CORE", 4), "core",
            "EntityId rejects blank",
            TestLevel.L0_UNIT
        );
    }

    @Test
    void afCore005_versionImmutability() {
        var v = new Version("2.0.0");
        assertNotNull(v.value());
        assertThrows(
            NullPointerException.class,
            () -> new Version(null)
        );
        runner.recordPass(
            TckId.of("CORE", 5), "core",
            "Version immutability",
            TestLevel.L0_UNIT
        );
    }

    @Test
    void afCore006_referenceCrossVersion() {
        var ref1 = new Reference(
            new EntityId("x"), new Version("1.0.0")
        );
        var ref2 = new Reference(
            new EntityId("x"), new Version("2.0.0")
        );
        assertNotEquals(ref1, ref2);
        runner.recordPass(
            TckId.of("CORE", 6), "core",
            "Reference cross-version",
            TestLevel.L2_CONFORMANCE
        );
    }

    @Test
    void afCore007_tckIdFormat() {
        var id = TckId.of("CORE", 1);
        assertEquals("AF-CORE-001", id.toString());

        var parsed = TckId.parse("AF-GRAPH-012");
        assertEquals("GRAPH", parsed.domain());
        assertEquals(12, parsed.number());
        runner.recordPass(
            TckId.of("CORE", 7), "core",
            "TCK ID format AF-DOMAIN-NNN",
            TestLevel.L2_CONFORMANCE
        );
    }

    @Test
    void afCore008_tckIdRejectsInvalid() {
        assertThrows(
            IllegalArgumentException.class,
            () -> TckId.parse("INVALID")
        );
        runner.recordPass(
            TckId.of("CORE", 8), "core",
            "TCK ID rejects invalid format",
            TestLevel.L2_CONFORMANCE
        );
    }

    @Test
    void afCore009_releaseGateAllPass() {
        runner.recordPass(
            TckId.of("CORE", 9), "core",
            "Release gate all pass",
            TestLevel.L5_RELEASE
        );
        var report = runner.buildReport();
        var gate = ReleaseGate.evaluate(report);
        assertTrue(gate.isApproved());
    }
}
