package io.github.regalpine.agentforge.arch;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * H7 Module Structure & Dependency Direction.
 * AF-RELEASE-01 §3, AF-AD-14 §12.4-§12.5.
 *
 * T-H7.4: Maven module structure completeness.
 * T-H7.5: Dependency direction enforcement.
 * T-H7.6: Core dependency constraints.
 */
class ModuleStructureTest {

    // ── T-H7.4: Module Structure Completeness ──

    /**
     * RELEASE-01 §3 / AF-AD-14 §12.4:
     * All modules must exist with correct
     * directory structure and pom.xml.
     */
    @Test
    void afMod001_allCoreModulesExist() {
        Path root = projectRoot();

        // Core
        assertModuleDir(root, "agentforge-kernel");
        assertModuleDir(root, "agentforge-agent");
        assertModuleDir(root, "agentforge-capability");
        assertModuleDir(root, "agentforge-graph");
        assertModuleDir(root, "agentforge-graph-ir");
        assertModuleDir(root, "agentforge-compiler");
    }

    @Test
    void afMod002_runtimeModulesExist() {
        Path root = projectRoot();
        assertModuleDir(root, "agentforge-runtime");
        assertModuleDir(root, "agentforge-event");
        assertModuleDir(root, "agentforge-persistence");
        assertModuleDir(root, "agentforge-distributed");
    }

    @Test
    void afMod003_platformModulesExist() {
        Path root = projectRoot();
        assertModuleDir(root, "agentforge-security");
        assertModuleDir(root, "agentforge-observability");
        assertModuleDir(root, "agentforge-server");
        assertModuleDir(root, "agentforge-cli");
        assertModuleDir(root, "agentforge-sdk");
    }

    @Test
    void afMod004_providerModulesExist() {
        Path root = projectRoot();
        assertModuleDir(root, "agentforge-provider-spi");
        assertModuleDir(root, "agentforge-provider-mock");
        assertModuleDir(root, "agentforge-provider-calculator");
        assertModuleDir(root, "agentforge-provider-http");
        assertModuleDir(root, "agentforge-provider-agent");
        assertModuleDir(root, "agentforge-provider-llm");
        assertModuleDir(root, "agentforge-provider-mcp");
    }

    @Test
    void afMod005_ecosystemModulesExist() {
        Path root = projectRoot();
        // Provider ecosystem (H0)
        assertModuleDir(root, "agentforge-provider-openai");
        assertModuleDir(root, "agentforge-provider-anthropic");
        assertModuleDir(root, "agentforge-provider-ollama");
        assertModuleDir(root, "agentforge-provider-openai-compatible");

        // Model (E0)
        assertModuleDir(root, "agentforge-model");

        // Capability adapters (H1)
        assertModuleDir(root, "agentforge-capability-http");
        assertModuleDir(root, "agentforge-capability-jdbc");
        assertModuleDir(root, "agentforge-capability-file");

        // Memory / RAG adapters (H2)
        assertModuleDir(root, "agentforge-memory");
        assertModuleDir(root, "agentforge-memory-postgresql");
        assertModuleDir(root, "agentforge-memory-redis");
        assertModuleDir(root, "agentforge-rag");
        assertModuleDir(root, "agentforge-rag-postgresql");
        assertModuleDir(root, "agentforge-rag-opensearch");
        assertModuleDir(root, "agentforge-rag-qdrant");
    }

    @Test
    void afMod006_qualityModulesExist() {
        Path root = projectRoot();
        assertModuleDir(root, "agentforge-multi-agent");
        assertModuleDir(root, "agentforge-evaluation");
        assertModuleDir(root, "agentforge-test-fault");
        assertModuleDir(root, "agentforge-benchmark");
        assertModuleDir(root, "agentforge-hardening-tck");
        assertModuleDir(root, "agentforge-architecture-tests");
        assertModuleDir(root, "agentforge-examples");
    }

    @Test
    void afMod007_infrastructureModulesExist() {
        Path root = projectRoot();
        assertModuleDir(root, "agentforge-bom");
        assertModuleDir(root, "agentforge-persistence-postgresql");
    }

    @Test
    void afMod008_enhance3ModulesExist() {
        Path root = projectRoot();
        // Enhance3 modules (X10-X11)
        assertModuleDir(root, "agentforge-service");
        assertModuleDir(root, "agentforge-multimodal");
        assertModuleDir(root, "agentforge-control-plane");
        assertModuleDir(root, "agentforge-evolution");
        // Debugger/TestKit (X3)
        assertModuleDir(root, "agentforge-debugger");
        assertModuleDir(root, "agentforge-testkit");
        // Workspace (X6)
        assertModuleDir(root, "agentforge-workspace");
    }

    @Test
    void afMod009_eachModuleHasPom() {
        Path root = projectRoot();
        String[] modules = {
            "agentforge-kernel", "agentforge-agent",
            "agentforge-capability", "agentforge-graph",
            "agentforge-runtime", "agentforge-event",
            "agentforge-bom", "agentforge-server",
            "agentforge-cli", "agentforge-sdk"
        };
        for (String mod : modules) {
            Path pom = root.resolve(mod).resolve("pom.xml");
            assertTrue(Files.exists(pom),
                "pom.xml must exist for " + mod);
        }
    }

    // ── T-H7.5: Dependency Direction ──

    /**
     * RELEASE-01 §3 / AF-AD-14 §12.5:
     * Core must not depend on Platform or
     * Infrastructure modules.
     */
    @Test
    void afDep001_kernelHasNoPlatformImports()
        throws Exception {
        assertNoForbiddenImports(
            "io.github.regalpine.agentforge.kernel",
            Set.of(
                "io.github.regalpine.agentforge.runtime",
                "io.github.regalpine.agentforge.persistence",
                "io.github.regalpine.agentforge.distributed",
                "io.github.regalpine.agentforge.security",
                "io.github.regalpine.agentforge.observability",
                "io.github.regalpine.agentforge.server",
                "io.github.regalpine.agentforge.cli",
                "io.github.regalpine.agentforge.compiler"
            )
        );
    }

    @Test
    void afDep002_graphHasNoRuntimeImports()
        throws Exception {
        assertNoForbiddenImports(
            "io.github.regalpine.agentforge.graph",
            Set.of(
                "io.github.regalpine.agentforge.runtime",
                "io.github.regalpine.agentforge.server",
                "io.github.regalpine.agentforge.persistence"
            )
        );
    }

    @Test
    void afDep003_compilerHasNoServerImports()
        throws Exception {
        assertNoForbiddenImports(
            "io.github.regalpine.agentforge.compiler",
            Set.of(
                "io.github.regalpine.agentforge.runtime",
                "io.github.regalpine.agentforge.server",
                "io.github.regalpine.agentforge.cli"
            )
        );
    }

    // ── T-H7.6: Core Dependency Constraints ──

    @Test
    void afDep004_coreHasNoFrameworkDeps()
        throws Exception {
        // Core must not depend on Spring, Hibernate,
        // Jackson, Micrometer, OpenTelemetry
        Set<String> forbidden = Set.of(
            "org.springframework",
            "org.hibernate",
            "com.fasterxml.jackson",
            "io.micrometer",
            "io.opentelemetry"
        );
        assertNoForbiddenImports(
            "io.github.regalpine.agentforge.kernel",
            forbidden
        );
        assertNoForbiddenImports(
            "io.github.regalpine.agentforge.graph",
            forbidden
        );
    }

    @Test
    void afDep005_providerSpiIsMinimal()
        throws Exception {
        // provider-spi should not import runtime
        assertNoForbiddenImports(
            "io.github.regalpine.agentforge.provider.spi",
            Set.of(
                "io.github.regalpine.agentforge.runtime",
                "io.github.regalpine.agentforge.server"
            )
        );
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
            if (Files.exists(
                    dir.getParent().resolve("agentforge-kernel"))) {
                return dir.getParent();
            }
        }
        return Path.of(".");
    }

    private void assertNoForbiddenImports(
        String sourcePackage,
        Set<String> forbidden
    ) throws Exception {
        ClassLoader cl = Thread.currentThread()
            .getContextClassLoader();
        String path = sourcePackage.replace('.', '/');
        Enumeration<URL> resources = cl.getResources(path);

        while (resources.hasMoreElements()) {
            URL resource = resources.nextElement();
            if (!"file".equals(resource.getProtocol())) {
                continue;
            }

            java.io.File dir = new java.io.File(resource.toURI());
            if (!dir.isDirectory()) continue;

            java.io.File[] classFiles = dir.listFiles(
                (d, name) -> name.endsWith(".class")
            );
            if (classFiles == null) continue;

            for (java.io.File classFile : classFiles) {
                byte[] bytes = Files.readAllBytes(
                    classFile.toPath()
                );
                String content = new String(
                    bytes, java.nio.charset.StandardCharsets.ISO_8859_1
                );

                for (String forbiddenPkg : forbidden) {
                    String forbiddenPath =
                        forbiddenPkg.replace('.', '/');
                    assertFalse(
                        content.contains(forbiddenPath),
                        "Class " + sourcePackage + "."
                        + classFile.getName()
                        + " references forbidden package: "
                        + forbiddenPkg
                    );
                }
            }
        }
    }
}
