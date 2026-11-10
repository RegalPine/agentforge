package io.github.regalpine.agentforge.workspace;

import io.github.regalpine.agentforge.security.DataClassification;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Workspace & Sandbox tests.
 * AF-ENHANCE-07 §76: TCK.
 *
 * <p>Covers: PathValidator, WorkspacePolicy,
 * SandboxPolicy, ResourceLimits, Artifact,
 * WorkspaceStatus, ArtifactStatus,
 * InMemoryWorkspace, InMemorySandbox,
 * ProcessRequest, ProcessResult.
 */
class WorkspaceSandboxTest {

    // ====================
    // PathValidator
    // ====================

    @Test
    void pathValidator_acceptsValidPath() {
        var v = new PathValidator(
            "/workspace/ws1"
        );
        String result = v.validate(
            "/workspace/ws1/input/data.csv"
        );
        assertEquals(
            "/workspace/ws1/input/data.csv",
            result
        );
    }

    @Test
    void pathValidator_rejectsTraversal() {
        var v = new PathValidator(
            "/workspace/ws1"
        );
        assertThrows(
            IllegalArgumentException.class,
            () -> v.validate(
                "/workspace/ws1/../../etc/passwd"
            )
        );
    }

    @Test
    void pathValidator_rejectsEscaping() {
        var v = new PathValidator(
            "/workspace/ws1"
        );
        assertFalse(
            v.isValid("/etc/passwd")
        );
    }

    @Test
    void pathValidator_resolvesRelative() {
        var v = new PathValidator(
            "/workspace/ws1"
        );
        String resolved = v.resolve(
            "input/data.csv"
        );
        assertEquals(
            "/workspace/ws1/input/data.csv",
            resolved
        );
    }

    @Test
    void pathValidator_handlesDots() {
        var v = new PathValidator(
            "/workspace/ws1"
        );
        String resolved = v.validate(
            "/workspace/ws1/a/b/../c"
        );
        assertEquals(
            "/workspace/ws1/a/c",
            resolved
        );
    }

    @Test
    void pathValidator_rejectsBlank() {
        var v = new PathValidator(
            "/workspace/ws1"
        );
        assertThrows(
            IllegalArgumentException.class,
            () -> v.validate("")
        );
    }

    @Test
    void pathValidator_getRoot() {
        var v = new PathValidator(
            "/workspace/ws1"
        );
        assertEquals(
            "/workspace/ws1",
            v.getWorkspaceRoot()
        );
    }

    // ====================
    // WorkspacePolicy
    // ====================

    @Test
    void workspacePolicy_defaultPerms() {
        var p = new WorkspacePolicy(
            "test", null, 0, 0, 0,
            null, null
        );
        assertTrue(p.hasPermission(
            WorkspacePolicy.WorkspacePermission.READ
        ));
        assertTrue(p.hasPermission(
            WorkspacePolicy.WorkspacePermission.WRITE
        ));
        assertFalse(p.hasPermission(
            WorkspacePolicy.WorkspacePermission.EXECUTE
        ));
        assertFalse(p.hasPermission(
            WorkspacePolicy.WorkspacePermission.EXPORT
        ));
        assertFalse(p.hasPermission(
            WorkspacePolicy.WorkspacePermission.DELETE
        ));
    }

    @Test
    void workspacePolicy_permissive() {
        var p = WorkspacePolicy.permissive();
        for (var perm
            : WorkspacePolicy.WorkspacePermission
                .values()
        ) {
            assertTrue(
                p.hasPermission(perm)
            );
        }
    }

    @Test
    void workspacePolicy_readOnly() {
        var p = WorkspacePolicy.readOnly();
        assertTrue(p.hasPermission(
            WorkspacePolicy.WorkspacePermission.READ
        ));
        assertTrue(p.hasPermission(
            WorkspacePolicy.WorkspacePermission.LIST
        ));
        assertFalse(p.hasPermission(
            WorkspacePolicy.WorkspacePermission.WRITE
        ));
    }

    @Test
    void workspacePolicy_defaults() {
        var p = new WorkspacePolicy(
            "test", null, 0, 0, 0,
            null, null
        );
        assertEquals(
            2L * 1024 * 1024 * 1024,
            p.maxStorageBytes()
        );
        assertEquals(
            100L * 1024 * 1024,
            p.maxFileSizeBytes()
        );
        assertEquals(10_000, p.maxFileCount());
    }

    // ====================
    // SandboxPolicy
    // ====================

    @Test
    void sandboxPolicy_restrictive() {
        var p =
            SandboxPolicy.restrictive();
        assertTrue(
            p.isExecutableAllowed("python")
        );
        assertTrue(
            p.isExecutableAllowed("java")
        );
        assertFalse(
            p.isExecutableAllowed("rm")
        );
        assertFalse(
            p.isHostAllowed("evil.com")
        );
    }

    @Test
    void sandboxPolicy_permissive() {
        var p =
            SandboxPolicy.permissive();
        assertTrue(
            p.isExecutableAllowed("anything")
        );
        assertTrue(
            p.isHostAllowed("any.host.com")
        );
    }

    @Test
    void sandboxPolicy_allowlistNetwork() {
        var p = new SandboxPolicy(
            "test", null,
            NetworkMode.ALLOWLIST,
            Set.of("api.example.com"),
            null, null
        );
        assertTrue(
            p.isHostAllowed("api.example.com")
        );
        assertFalse(
            p.isHostAllowed("evil.com")
        );
    }

    @Test
    void sandboxPolicy_emptyExecAllowsAll() {
        var p = new SandboxPolicy(
            "test", Set.of(),
            null, null, null, null
        );
        assertTrue(
            p.isExecutableAllowed("anything")
        );
    }

    @Test
    void sandboxPolicy_defaults() {
        var p = new SandboxPolicy(
            "test", null, null, null,
            null, null
        );
        assertEquals(
            NetworkMode.DENY,
            p.networkMode()
        );
        assertEquals(
            SandboxIsolationLevel.LOGICAL,
            p.isolationLevel()
        );
    }

    // ====================
    // ResourceLimits
    // ====================

    @Test
    void resourceLimits_defaults() {
        var r = ResourceLimits.defaults();
        assertEquals(1, r.cpuCores());
        assertEquals(512, r.memoryMb());
        assertEquals(1024, r.diskMb());
        assertEquals(1000, r.maxFiles());
        assertEquals(4, r.maxProcesses());
        assertEquals(
            Duration.ofSeconds(120),
            r.timeout()
        );
    }

    @Test
    void resourceLimits_small() {
        var r = ResourceLimits.small();
        assertEquals(1, r.cpuCores());
        assertEquals(128, r.memoryMb());
        assertEquals(256, r.diskMb());
    }

    @Test
    void resourceLimits_large() {
        var r = ResourceLimits.large();
        assertEquals(4, r.cpuCores());
        assertEquals(4096, r.memoryMb());
    }

    @Test
    void resourceLimits_checks() {
        var r = ResourceLimits.defaults();
        assertTrue(r.isMemoryWithin(256));
        assertFalse(r.isMemoryWithin(1024));
        assertTrue(r.isFileCountWithin(500));
        assertFalse(r.isFileCountWithin(2000));
    }

    @Test
    void resourceLimits_correctsInvalid() {
        var r = new ResourceLimits(
            -1, -1, -1, -1, -1, null, -1
        );
        assertEquals(1, r.cpuCores());
        assertEquals(512, r.memoryMb());
        assertEquals(1024, r.diskMb());
        assertEquals(1000, r.maxFiles());
        assertEquals(4, r.maxProcesses());
    }

    // ====================
    // Artifact
    // ====================

    @Test
    void artifact_builder() {
        Artifact a = Artifact.builder()
            .artifactId(
                new ArtifactId("art-001")
            )
            .name("report.pdf")
            .contentType("application/pdf")
            .size(1024)
            .digest("sha256:abc123")
            .classification(
                DataClassification.CONFIDENTIAL
            )
            .build();

        assertEquals(
            "art-001",
            a.artifactId().value()
        );
        assertEquals("report.pdf", a.name());
        assertEquals(
            "application/pdf",
            a.contentType()
        );
        assertEquals(
            ArtifactStatus.CREATED,
            a.status()
        );
        assertEquals(
            DataClassification.CONFIDENTIAL,
            a.classification()
        );
    }

    @Test
    void artifact_withStatus() {
        Artifact a = Artifact.builder()
            .artifactId(
                new ArtifactId("art-001")
            )
            .name("report.pdf")
            .build();

        Artifact b = a.withStatus(
            ArtifactStatus.READY
        );

        assertEquals(
            ArtifactStatus.CREATED,
            a.status()
        );
        assertEquals(
            ArtifactStatus.READY,
            b.status()
        );
        assertNotSame(a, b);
    }

    @Test
    void artifact_defaults() {
        Artifact a = new Artifact(
            new ArtifactId("a1"),
            null, null, null,
            "test.txt", null, 0, null,
            null, null, null, null, null
        );
        assertEquals(
            "application/octet-stream",
            a.contentType()
        );
        assertEquals(
            DataClassification.INTERNAL,
            a.classification()
        );
        assertNotNull(a.metadata());
        assertTrue(a.metadata().isEmpty());
    }

    // ====================
    // WorkspaceStatus
    // ====================

    @Test
    void workspaceStatus_accessible() {
        assertFalse(
            WorkspaceStatus.CREATED
                .isAccessible()
        );
        assertFalse(
            WorkspaceStatus.INITIALIZED
                .isAccessible()
        );
        assertTrue(
            WorkspaceStatus.READY
                .isAccessible()
        );
        assertTrue(
            WorkspaceStatus.ACTIVE
                .isAccessible()
        );
        assertFalse(
            WorkspaceStatus.FROZEN
                .isAccessible()
        );
        assertFalse(
            WorkspaceStatus.ARCHIVED
                .isAccessible()
        );
    }

    @Test
    void workspaceStatus_terminal() {
        assertFalse(
            WorkspaceStatus.READY
                .isTerminal()
        );
        assertTrue(
            WorkspaceStatus.DELETED
                .isTerminal()
        );
    }

    // ====================
    // ArtifactStatus
    // ====================

    @Test
    void artifactStatus_available() {
        assertFalse(
            ArtifactStatus.CREATED
                .isAvailable()
        );
        assertFalse(
            ArtifactStatus.STAGED
                .isAvailable()
        );
        assertTrue(
            ArtifactStatus.READY
                .isAvailable()
        );
        assertTrue(
            ArtifactStatus.REFERENCED
                .isAvailable()
        );
        assertFalse(
            ArtifactStatus.EXPIRED
                .isAvailable()
        );
    }

    @Test
    void artifactStatus_terminal() {
        assertTrue(
            ArtifactStatus.DELETED
                .isTerminal()
        );
        assertTrue(
            ArtifactStatus.INVALID
                .isTerminal()
        );
        assertTrue(
            ArtifactStatus.CORRUPTED
                .isTerminal()
        );
        assertTrue(
            ArtifactStatus.REVOKED
                .isTerminal()
        );
        assertFalse(
            ArtifactStatus.READY
                .isTerminal()
        );
    }

    // ====================
    // InMemoryWorkspace
    // ====================

    @Test
    void workspace_createAndRead() {
        var ws = InMemoryWorkspace.builder()
            .id("ws1")
            .build();

        ws.write(
            "hello.txt",
            "world".getBytes(
                StandardCharsets.UTF_8
            )
        );

        assertTrue(ws.exists("hello.txt"));
        assertEquals(
            "world",
            new String(
                ws.read("hello.txt"),
                StandardCharsets.UTF_8
            )
        );
    }

    @Test
    void workspace_seedFiles() {
        var ws = InMemoryWorkspace.builder()
            .id("ws1")
            .file(
                "/input/data.csv",
                "a,b\n1,2"
            )
            .build();

        assertTrue(
            ws.exists("/input/data.csv")
        );
        assertEquals(
            "a,b\n1,2",
            new String(
                ws.read("/input/data.csv"),
                StandardCharsets.UTF_8
            )
        );
    }

    @Test
    void workspace_delete() {
        var ws = InMemoryWorkspace.builder()
            .id("ws1")
            .file("test.txt", "hello")
            .build();

        assertTrue(ws.exists("test.txt"));
        ws.delete("test.txt");
        assertFalse(ws.exists("test.txt"));
    }

    @Test
    void workspace_list() {
        var ws = InMemoryWorkspace.builder()
            .id("ws1")
            .file("/input/a.txt", "a")
            .file("/input/b.txt", "b")
            .file("/output/c.txt", "c")
            .build();

        List<String> items =
            ws.list("/input");
        assertEquals(2, items.size());
        assertTrue(items.contains("a.txt"));
        assertTrue(items.contains("b.txt"));
    }

    @Test
    void workspace_createDirectory() {
        var ws = InMemoryWorkspace.builder()
            .id("ws1")
            .build();

        ws.createDirectory("/data");
        assertTrue(ws.exists("/data"));
    }

    @Test
    void workspace_storageUsed() {
        var ws = InMemoryWorkspace.builder()
            .id("ws1")
            .build();

        assertEquals(0, ws.storageUsed());
        assertEquals(0, ws.fileCount());

        ws.write(
            "a.txt",
            "hello".getBytes(
                StandardCharsets.UTF_8
            )
        );
        assertEquals(5, ws.storageUsed());
        assertEquals(1, ws.fileCount());
    }

    @Test
    void workspace_rejectsTraversal() {
        var ws = InMemoryWorkspace.builder()
            .id("ws1")
            .build();

        assertThrows(
            IllegalArgumentException.class,
            () -> ws.write(
                "../../etc/passwd",
                "bad".getBytes()
            )
        );
    }

    @Test
    void workspace_enforcesPolicy() {
        var policy =
            WorkspacePolicy.readOnly();

        var ws = InMemoryWorkspace.builder()
            .id("ws1")
            .policy(policy)
            .build();

        assertThrows(
            SecurityException.class,
            () -> ws.write(
                "test.txt",
                "data".getBytes()
            )
        );
    }

    @Test
    void workspace_enforcesDeletePolicy() {
        var policy = new WorkspacePolicy(
            "no-delete",
            Set.of(
                WorkspacePolicy.WorkspacePermission.READ,
                WorkspacePolicy.WorkspacePermission.WRITE,
                WorkspacePolicy.WorkspacePermission.LIST
            ),
            0, 0, 0, null, null
        );

        var ws = InMemoryWorkspace.builder()
            .id("ws1")
            .policy(policy)
            .file("test.txt", "data")
            .build();

        assertThrows(
            SecurityException.class,
            () -> ws.delete("test.txt")
        );
    }

    @Test
    void workspace_notAccessibleWhenDeleted() {
        var ws = InMemoryWorkspace.builder()
            .id("ws1")
            .build();

        ws.transitionTo(
            WorkspaceStatus.DELETED
        );

        assertThrows(
            IllegalStateException.class,
            () -> ws.read("test.txt")
        );
    }

    @Test
    void workspace_artifacts() {
        var ws = InMemoryWorkspace.builder()
            .id("ws1")
            .build();

        Artifact a = Artifact.builder()
            .artifactId(
                new ArtifactId("art-001")
            )
            .name("report.pdf")
            .build();

        ws.registerArtifact(a);

        assertEquals(
            a, ws.getArtifact(
                new ArtifactId("art-001")
            )
        );
        assertEquals(
            1, ws.listArtifacts().size()
        );
    }

    @Test
    void workspace_fileSizeLimit() {
        var policy = new WorkspacePolicy(
            "small",
            null,
            0, 10, 100,
            null, null
        );

        var ws = InMemoryWorkspace.builder()
            .id("ws1")
            .policy(policy)
            .build();

        assertThrows(
            IllegalStateException.class,
            () -> ws.write(
                "big.txt",
                "12345678901".getBytes()
            )
        );
    }

    @Test
    void workspace_fileCountLimit() {
        var policy = new WorkspacePolicy(
            "limited",
            null,
            0, 0, 2,
            null, null
        );

        var ws = InMemoryWorkspace.builder()
            .id("ws1")
            .policy(policy)
            .build();

        ws.write("a.txt", "a".getBytes());
        ws.write("b.txt", "b".getBytes());

        assertThrows(
            IllegalStateException.class,
            () -> ws.write(
                "c.txt", "c".getBytes()
            )
        );
    }

    // ====================
    // InMemorySandbox
    // ====================

    @Test
    void sandbox_lifecycle() {
        var ws = InMemoryWorkspace.builder()
            .id("ws1")
            .build();

        var sb = InMemorySandbox.builder()
            .id("sb1")
            .workspace(ws)
            .build();

        assertEquals(
            SandboxStatus.CREATED,
            sb.status()
        );

        sb.start();
        assertEquals(
            SandboxStatus.RUNNING,
            sb.status()
        );

        sb.stop();
        assertEquals(
            SandboxStatus.STOPPED,
            sb.status()
        );

        sb.destroy();
        assertEquals(
            SandboxStatus.DESTROYED,
            sb.status()
        );
    }

    @Test
    void sandbox_execute() {
        var ws = InMemoryWorkspace.builder()
            .id("ws1")
            .build();

        var sb = InMemorySandbox.builder()
            .id("sb1")
            .workspace(ws)
            .build();

        sb.start();

        ProcessResult result = sb.execute(
            ProcessRequest.of(
                "python", "script.py"
            )
        );

        assertTrue(result.isSuccess());
        assertEquals(0, result.exitCode());
    }

    @Test
    void sandbox_rejectsWhenNotRunning() {
        var ws = InMemoryWorkspace.builder()
            .id("ws1")
            .build();

        var sb = InMemorySandbox.builder()
            .id("sb1")
            .workspace(ws)
            .build();

        assertThrows(
            IllegalStateException.class,
            () -> sb.execute(
                ProcessRequest.of("python")
            )
        );
    }

    @Test
    void sandbox_rejectsDisallowedExec() {
        var ws = InMemoryWorkspace.builder()
            .id("ws1")
            .build();

        var sb = InMemorySandbox.builder()
            .id("sb1")
            .workspace(ws)
            .policy(
                SandboxPolicy.restrictive()
            )
            .build();

        sb.start();

        assertThrows(
            SecurityException.class,
            () -> sb.execute(
                ProcessRequest.of("rm")
            )
        );
    }

    @Test
    void sandbox_mockResult() {
        var ws = InMemoryWorkspace.builder()
            .id("ws1")
            .build();

        var sb = InMemorySandbox.builder()
            .id("sb1")
            .workspace(ws)
            .build();

        sb.start();

        sb.mockResult(
            "python",
            ProcessResult.success(
                "mock-1",
                "hello world",
                Duration.ofMillis(5)
            )
        );

        ProcessResult r = sb.execute(
            ProcessRequest.of("python")
        );

        assertEquals("mock-1", r.processId());
        assertEquals(
            "hello world", r.stdout()
        );
    }

    @Test
    void sandbox_mountsWorkspace() {
        var ws = InMemoryWorkspace.builder()
            .id("ws1")
            .build();

        var sb = InMemorySandbox.builder()
            .id("sb1")
            .workspace(ws)
            .build();

        assertSame(ws, sb.workspace());
        assertEquals(
            "ws1",
            sb.workspace().id().value()
        );
    }

    // ====================
    // ProcessRequest
    // ====================

    @Test
    void processRequest_of() {
        var r = ProcessRequest.of(
            "python",
            "script.py", "--input",
            "/data/input.csv"
        );
        assertEquals("python", r.executable());
        assertEquals(3, r.arguments().size());
    }

    @Test
    void processRequest_rejectsBlank() {
        assertThrows(
            IllegalArgumentException.class,
            () -> ProcessRequest.of("")
        );
    }

    @Test
    void processRequest_nullArgs() {
        var r = new ProcessRequest(
            "python", null, null, null
        );
        assertTrue(r.arguments().isEmpty());
        assertTrue(r.environment().isEmpty());
    }

    // ====================
    // ProcessResult
    // ====================

    @Test
    void processResult_success() {
        var r = ProcessResult.success(
            "p1", "output",
            Duration.ofMillis(10)
        );
        assertTrue(r.isSuccess());
        assertEquals(0, r.exitCode());
        assertFalse(r.timedOut());
        assertFalse(r.killed());
    }

    @Test
    void processResult_failure() {
        var r = ProcessResult.failure(
            "p1", 1, "error",
            Duration.ofMillis(10)
        );
        assertFalse(r.isSuccess());
        assertEquals(1, r.exitCode());
        assertEquals("error", r.stderr());
    }

    @Test
    void processResult_timeout() {
        var r = ProcessResult.timeout(
            "p1", Duration.ofSeconds(120)
        );
        assertFalse(r.isSuccess());
        assertTrue(r.timedOut());
        assertTrue(r.killed());
    }

    // ====================
    // WorkspaceId / ArtifactId / SandboxId
    // ====================

    @Test
    void workspaceId_rejectsBlank() {
        assertThrows(
            IllegalArgumentException.class,
            () -> new WorkspaceId("")
        );
        assertThrows(
            NullPointerException.class,
            () -> new WorkspaceId(null)
        );
    }

    @Test
    void artifactId_rejectsBlank() {
        assertThrows(
            IllegalArgumentException.class,
            () -> new ArtifactId("")
        );
    }

    @Test
    void sandboxId_rejectsBlank() {
        assertThrows(
            IllegalArgumentException.class,
            () -> new SandboxId("")
        );
    }

    // ====================
    // WorkspaceScope
    // ====================

    @Test
    void workspaceScope_values() {
        assertEquals(
            5,
            WorkspaceScope.values().length
        );
        assertNotNull(
            WorkspaceScope.EXECUTION
        );
        assertNotNull(
            WorkspaceScope.TENANT
        );
    }

    // ====================
    // ResourceType
    // ====================

    @Test
    void resourceType_values() {
        assertEquals(
            6,
            ResourceType.values().length
        );
        assertNotNull(ResourceType.FILE);
        assertNotNull(ResourceType.ARTIFACT);
        assertNotNull(ResourceType.TEMPORARY);
    }

    // ====================
    // NetworkMode / SandboxIsolationLevel
    // ====================

    @Test
    void networkMode_values() {
        assertEquals(
            3, NetworkMode.values().length
        );
    }

    @Test
    void isolationLevel_values() {
        assertEquals(
            4,
            SandboxIsolationLevel
                .values().length
        );
    }
}
