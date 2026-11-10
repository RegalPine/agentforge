package io.github.regalpine.agentforge.arch;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URL;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Verifies that Core modules (kernel, agent, capability, graph, graph-ir)
 * do not depend on Platform or Infrastructure modules.
 *
 * Allowed dependency direction:
 *   kernel → agent → capability → graph → graph-ir → compiler → runtime
 *
 * Forbidden:
 *   kernel → runtime, compiler, persistence, distributed, security, server, cli, studio
 *   graph → runtime, executor, server
 *   compiler → runtime, provider implementation
 */
class DependencyDirectionTest {

    private static final Set<String> CORE_PACKAGES = Set.of(
            "io.github.regalpine.agentforge.kernel",
            "io.github.regalpine.agentforge.agent",
            "io.github.regalpine.agentforge.capability",
            "io.github.regalpine.agentforge.graph",
            "io.github.regalpine.agentforge.graph.ir"
    );

    private static final Set<String> FORBIDDEN_IN_CORE = Set.of(
            "io.github.regalpine.agentforge.runtime",
            "io.github.regalpine.agentforge.persistence",
            "io.github.regalpine.agentforge.distributed",
            "io.github.regalpine.agentforge.security",
            "io.github.regalpine.agentforge.observability",
            "io.github.regalpine.agentforge.server",
            "io.github.regalpine.agentforge.cli",
            "io.github.regalpine.agentforge.compiler",
            "io.github.regalpine.agentforge.provider"
    );

    private static final Set<String> FORBIDDEN_FRAMEWORKS = Set.of(
            "org.springframework",
            "org.hibernate",
            "com.fasterxml.jackson",
            "io.micrometer",
            "io.opentelemetry"
    );

    @Test
    void kernelClassesShouldNotImportRuntimePackages() throws Exception {
        assertNoForbiddenImports("io.github.regalpine.agentforge.kernel", FORBIDDEN_IN_CORE);
    }

    @Test
    void graphClassesShouldNotImportRuntimePackages() throws Exception {
        assertNoForbiddenImports("io.github.regalpine.agentforge.graph", FORBIDDEN_IN_CORE);
    }

    @Test
    void compilerClassesShouldNotImportRuntimePackages() throws Exception {
        assertNoForbiddenImports("io.github.regalpine.agentforge.compiler",
                Set.of("io.github.regalpine.agentforge.runtime",
                        "io.github.regalpine.agentforge.server",
                        "io.github.regalpine.agentforge.cli"));
    }

    @Test
    void coreShouldNotDependOnForbiddenFrameworks() throws Exception {
        for (String corePkg : CORE_PACKAGES) {
            assertNoForbiddenImports(corePkg, FORBIDDEN_FRAMEWORKS);
        }
    }

    private void assertNoForbiddenImports(String sourcePackage, Set<String> forbidden) throws Exception {
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        String path = sourcePackage.replace('.', '/');
        Enumeration<URL> resources = cl.getResources(path);

        int[] scannedCount = {0};

        while (resources.hasMoreElements()) {
            URL resource = resources.nextElement();
            if ("file".equals(resource.getProtocol())) {
                java.io.File dir = new java.io.File(resource.toURI());
                if (dir.isDirectory()) {
                    scanDirectoryRecursive(dir, sourcePackage, forbidden, scannedCount);
                }
            } else if ("jar".equals(resource.getProtocol())) {
                String jarPath = resource.getPath();
                int bangIdx = jarPath.indexOf('!');
                if (bangIdx > 0) {
                    String jarFile = jarPath.substring(5, bangIdx); // strip "file:"
                    try (java.util.jar.JarFile jf = new java.util.jar.JarFile(jarFile)) {
                        String prefix = path + "/";
                        var entries = jf.entries();
                        while (entries.hasMoreElements()) {
                            java.util.jar.JarEntry entry = entries.nextElement();
                            String name = entry.getName();
                            if (name.startsWith(prefix) && name.endsWith(".class") && !entry.isDirectory()) {
                                try (var is = jf.getInputStream(entry)) {
                                    byte[] bytes = is.readAllBytes();
                                    checkClassBytes(bytes, sourcePackage + "." + name.substring(prefix.length(), name.length() - 6).replace('/', '.'), forbidden);
                                    scannedCount[0]++;
                                }
                            }
                        }
                    }
                }
            }
        }

        assertTrue(scannedCount[0] > 0,
                "Architecture scan found no classes for package: " + sourcePackage
                        + ". Scan coverage must be non-zero.");
    }

    private void scanDirectoryRecursive(java.io.File dir, String packageName, Set<String> forbidden, int[] count) throws Exception {
        java.io.File[] classFiles = dir.listFiles((d, name) -> name.endsWith(".class") || d.isDirectory());
        if (classFiles == null) return;

        for (java.io.File classFile : classFiles) {
            if (classFile.isDirectory()) {
                String subPkg = packageName + "." + classFile.getName();
                scanDirectoryRecursive(classFile, subPkg, forbidden, count);
            } else {
                byte[] bytes = java.nio.file.Files.readAllBytes(classFile.toPath());
                String className = packageName + "." + classFile.getName().replace(".class", "");
                checkClassBytes(bytes, className, forbidden);
                count[0]++;
            }
        }
    }

    private void checkClassBytes(byte[] bytes, String className, Set<String> forbidden) {
        String content = new String(bytes, java.nio.charset.StandardCharsets.ISO_8859_1);
        for (String forbiddenPkg : forbidden) {
            String forbiddenPath = forbiddenPkg.replace('.', '/');
            assertFalse(content.contains(forbiddenPath),
                    "Class " + className + " contains reference to forbidden package: " + forbiddenPkg);
        }
    }
}
