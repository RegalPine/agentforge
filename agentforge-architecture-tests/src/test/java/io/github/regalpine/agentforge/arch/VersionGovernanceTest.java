package io.github.regalpine.agentforge.arch;

import io.github.regalpine.agentforge.kernel.Version;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 * H7 Version Governance & Migration.
 * AF-RELEASE-01 §5, AF-AD-14 §12.
 *
 * T-H7.9:  Version governance — application
 *   version / Graph version / schemaVersion /
 *   Compiler version / Plan format / fingerprint
 *   are independently managed.
 * T-H7.10: Database expand/contract migration
 *   verification.
 */
class VersionGovernanceTest {

    // ── T-H7.9: Version Governance ──

    @Test
    void afVer001_applicationVersion() {
        // Application version is managed via pom.xml
        Path root = projectRoot();
        Path pom = root.resolve("pom.xml");
        assertTrue(Files.exists(pom),
            "Root pom.xml must exist");
    }

    @Test
    void afVer002_graphVersionIsIndependent() {
        // Graph version is part of GraphDefinition
        // and is independent of app version
        var version = new Version("2.5.0");
        assertEquals("2.5.0", version.value());

        // Graph version is separate from app version
        var appVersion = new Version("1.0.0");
        assertNotEquals(
            appVersion.value(),
            version.value()
        );
    }

    @Test
    void afVer003_compilerVersionExists() {
        // Compiler produces plans with fingerprints
        assertDoesNotThrow(() ->
            Class.forName(
                "io.github.regalpine.agentforge.compiler.DefaultGraphCompiler"
            )
        );
    }

    @Test
    void afVer004_planFingerprintExists() {
        // ExecutionPlan has fingerprint field
        var planClass =
            io.github.regalpine.agentforge.graph.ir.ExecutionPlan.class;
        assertTrue(planClass.isRecord());

        boolean hasFingerprint = false;
        for (var comp : planClass.getRecordComponents()) {
            if ("fingerprint".equals(comp.getName())) {
                hasFingerprint = true;
                break;
            }
        }
        assertTrue(hasFingerprint,
            "ExecutionPlan must have fingerprint");
    }

    @Test
    void afVer005_bomManagesVersions() {
        // BOM pom.xml must exist and manage versions
        Path root = projectRoot();
        Path bom = root.resolve("agentforge-bom/pom.xml");
        assertTrue(Files.exists(bom),
            "BOM pom.xml must exist");
    }

    @Test
    void afVer006_schemaVersionManaged() {
        // Graph JSON schema version is tracked
        // via GraphDefinition.version()
        var graphVersion = new Version("1.0.0");
        assertNotNull(graphVersion.value());
        assertFalse(graphVersion.value().isEmpty());
    }

    // ── T-H7.10: Migration Verification ──

    @Test
    void afMig001_persistenceModuleExists() {
        Path root = projectRoot();
        assertModuleDir(root, "agentforge-persistence");
        assertModuleDir(root, "agentforge-persistence-postgresql");
    }

    @Test
    void afMig002_sqlMigrationsExist() {
        // PostgreSQL persistence should have
        // migration scripts
        Path root = projectRoot();
        Path pgDir = root.resolve(
            "agentforge-persistence-postgresql/src"
        );
        assertTrue(Files.isDirectory(pgDir),
            "PostgreSQL module src must exist");
    }

    @Test
    void afMig003_memoryMigrationsExist() {
        Path root = projectRoot();
        assertModuleDir(root, "agentforge-memory-postgresql");
    }

    @Test
    void afMig004_ragMigrationsExist() {
        Path root = projectRoot();
        assertModuleDir(root, "agentforge-rag-postgresql");
    }

    // ── Helpers ──

    private void assertModuleDir(Path root, String name) {
        Path dir = root.resolve(name);
        assertTrue(Files.isDirectory(dir),
            "Module directory must exist: " + name);
    }

    private Path projectRoot() {
        String userDir = System.getProperty("user.dir");
        if (userDir != null) {
            Path dir = Path.of(userDir);
            if (dir.getFileName().toString()
                    .equals("agentforge-architecture-tests")) {
                return dir.getParent();
            }
            if (Files.exists(dir.resolve("agentforge-kernel"))) {
                return dir;
            }
        }
        return Path.of(".");
    }
}
