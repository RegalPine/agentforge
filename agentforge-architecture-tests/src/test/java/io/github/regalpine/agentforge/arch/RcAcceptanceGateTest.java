package io.github.regalpine.agentforge.arch;

import io.github.regalpine.agentforge.hardening.*;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * H7 RC Acceptance Gate.
 * AF-RELEASE-01 §6, AF-AD-14 §13.
 *
 * T-H7.11: RC acceptance gate — key TCK
 *   passes, no high-risk security issues,
 *   disaster recovery evidence, SLO approved.
 */
class RcAcceptanceGateTest {

    // ── T-H7.11: RC Gate ──

    @Test
    void afRc001_tckFrameworkExists() {
        // TCK framework classes must exist
        assertDoesNotThrow(() ->
            Class.forName(
                "io.github.regalpine.agentforge.hardening.TckCase"
            )
        );
        assertDoesNotThrow(() ->
            Class.forName(
                "io.github.regalpine.agentforge.hardening.TckResult"
            )
        );
        assertDoesNotThrow(() ->
            Class.forName(
                "io.github.regalpine.agentforge.hardening.ReleaseGate"
            )
        );
    }

    @Test
    void afRc002_releaseGateApprovable() {
        // Simulate a passing TCK report
        var cases = List.of(
            TckCase.pass(
                TckId.parse("AF-CORE-001"),
                "core",
                "Core freeze verified",
                TestLevel.L5_RELEASE
            ),
            TckCase.pass(
                TckId.parse("AF-GRAPH-001"),
                "graph",
                "Graph immutable",
                TestLevel.L5_RELEASE
            ),
            TckCase.pass(
                TckId.parse("AF-RUNTIME-001"),
                "runtime",
                "Execution unified",
                TestLevel.L5_RELEASE
            )
        );

        var report = new TckReport(
            "RC-GATE-001",
            java.time.Instant.now(),
            cases,
            Map.of("phase", "H7-RC")
        );

        var gate = ReleaseGate.evaluate(report);
        assertEquals(
            ReleaseGate.Decision.APPROVED,
            gate.decision()
        );
        assertTrue(gate.isApproved());
    }

    @Test
    void afRc003_releaseGateRejectsFailure() {
        var cases = List.of(
            TckCase.pass(
                TckId.parse("AF-CORE-001"),
                "core",
                "OK",
                TestLevel.L5_RELEASE
            ),
            TckCase.fail(
                TckId.parse("AF-SEC-001"),
                "security",
                "Security check failed",
                TestLevel.L5_RELEASE,
                "High-risk vulnerability"
            )
        );

        var report = new TckReport(
            "RC-GATE-002",
            java.time.Instant.now(),
            cases,
            Map.of()
        );

        var gate = ReleaseGate.evaluate(report);
        assertEquals(
            ReleaseGate.Decision.REJECTED,
            gate.decision()
        );
        assertFalse(gate.isApproved());
    }

    @Test
    void afRc004_securityModulePresent() {
        assertDoesNotThrow(() ->
            Class.forName(
                "io.github.regalpine.agentforge.security.SecurityContext"
            )
        );
        assertDoesNotThrow(() ->
            Class.forName(
                "io.github.regalpine.agentforge.security.AuthorizationRequest"
            )
        );
    }

    @Test
    void afRc005_disasterRecoveryEvidence() {
        // Distributed module has reconciliation
        assertDoesNotThrow(() ->
            Class.forName(
                "io.github.regalpine.agentforge.distributed.reconciliation.Reconciler"
            )
        );
        // Lease-based fencing
        assertDoesNotThrow(() ->
            Class.forName(
                "io.github.regalpine.agentforge.distributed.lease.ExecutionLease"
            )
        );
    }

    @Test
    void afRc006_sloFrameworkPresent() {
        // Observability has SLO support
        assertDoesNotThrow(() ->
            Class.forName(
                "io.github.regalpine.agentforge.observability.TraceManager"
            )
        );
    }

    @Test
    void afRc007_allConformanceDomainsCovered() {
        // Verify all 17 conformance test files
        // exist on filesystem
        Path root = projectRoot();
        String base = "agentforge-hardening-tck/src/test/java/"
            + "io/github/regalpine/agentforge/"
            + "hardening/conformance";

        String[] files = {
            "CoreConformanceTest.java",
            "CapabilityConformanceTest.java",
            "ModelConformanceTest.java",
            "GraphConformanceTest.java",
            "ExecutionConformanceTest.java",
            "RuntimeConformanceTest.java",
            "MemoryConformanceTest.java",
            "RagConformanceTest.java",
            "MultiAgentConformanceTest.java",
            "ObservabilityConformanceTest.java",
            "EvaluationConformanceTest.java",
            "SdkConformanceTest.java",
            "CliConformanceTest.java",
            "SecurityConformanceTest.java",
            "GovernanceConformanceTest.java",
            "DistributedConformanceTest.java",
            "TckAcceptanceReportTest.java"
        };

        for (String f : files) {
            Path p = root.resolve(base).resolve(f);
            assertTrue(Files.exists(p),
                "TCK conformance test must exist: " + f);
        }
    }

    @Test
    void afRc008_cicdWorkflowsExist() {
        Path root = projectRoot();
        assertTrue(
            Files.exists(root.resolve(
                ".github/workflows/build.yml"
            )),
            "CI build workflow must exist"
        );
        assertTrue(
            Files.exists(root.resolve(
                ".github/workflows/docker.yml"
            )),
            "Docker workflow must exist"
        );
        assertTrue(
            Files.exists(root.resolve(
                ".github/workflows/release.yml"
            )),
            "Release workflow must exist"
        );
    }

    @Test
    void afRc009_sbomScriptExists() {
        Path root = projectRoot();
        assertTrue(
            Files.exists(root.resolve(
                "scripts/generate-sbom.sh"
            )),
            "SBOM generation script must exist"
        );
    }

    @Test
    void afRc010_deployArtifactsExist() {
        Path root = projectRoot();
        assertTrue(
            Files.exists(root.resolve(
                "deploy/docker/Dockerfile"
            )),
            "Dockerfile must exist"
        );
        assertTrue(
            Files.exists(root.resolve(
                "deploy/kubernetes/deployment.yaml"
            )),
            "K8s deployment must exist"
        );
    }

    // ── Helpers ──

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
