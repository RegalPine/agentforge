package io.github.regalpine.agentforge.hardening.conformance;

import io.github.regalpine.agentforge.workspace.*;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * conformance-workspace-sandbox:
 * Workspace / Sandbox / Artifact /
 * Policy.
 * AF-AD-16 §7, T-X12.7.
 */
class WorkspaceSandboxConformanceTest {

    // ── WorkspaceId ──

    @Test
    void afWs001_workspaceId() {
        var id = new WorkspaceId(
            "ws-1"
        );
        assertEquals("ws-1",
            id.value());
    }

    @Test
    void afWs002_workspaceIdRejectsBlank() {
        assertThrows(
            NullPointerException.class,
            () -> new WorkspaceId(null)
        );
        assertThrows(
            IllegalArgumentException.class,
            () -> new WorkspaceId("")
        );
    }

    // ── WorkspaceScope ──

    @Test
    void afWs003_fiveScopes() {
        assertEquals(
            5,
            WorkspaceScope.values().length
        );
        assertNotNull(
            WorkspaceScope.EXECUTION
        );
        assertNotNull(
            WorkspaceScope.SESSION
        );
        assertNotNull(
            WorkspaceScope.AGENT
        );
        assertNotNull(
            WorkspaceScope.APPLICATION
        );
        assertNotNull(
            WorkspaceScope.TENANT
        );
    }

    // ── WorkspaceStatus ──

    @Test
    void afWs004_sevenStatuses() {
        assertEquals(
            7,
            WorkspaceStatus.values().length
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
        assertTrue(
            WorkspaceStatus.DELETED
                .isTerminal()
        );
    }

    // ── Workspace Interface ──

    @Test
    void afWs005_workspaceIsInterface() {
        assertTrue(
            Workspace.class.isInterface()
        );
    }

    // ── ArtifactId ──

    @Test
    void afWs006_artifactId() {
        var id = new ArtifactId(
            "art-1"
        );
        assertEquals("art-1",
            id.value());
    }

    @Test
    void afWs007_artifactIdRejectsBlank() {
        assertThrows(
            NullPointerException.class,
            () -> new ArtifactId(null)
        );
        assertThrows(
            IllegalArgumentException.class,
            () -> new ArtifactId("")
        );
    }

    // ── ArtifactStatus ──

    @Test
    void afWs008_artifactStatuses() {
        assertTrue(
            ArtifactStatus.values().length
                >= 4
        );
    }

    // ── SandboxId ──

    @Test
    void afWs009_sandboxId() {
        var id = new SandboxId(
            "sb-1"
        );
        assertEquals("sb-1",
            id.value());
    }

    @Test
    void afWs010_sandboxIdRejectsBlank() {
        assertThrows(
            NullPointerException.class,
            () -> new SandboxId(null)
        );
        assertThrows(
            IllegalArgumentException.class,
            () -> new SandboxId("")
        );
    }

    // ── SandboxStatus ──

    @Test
    void afWs011_eightSandboxStatuses() {
        assertEquals(
            8,
            SandboxStatus.values().length
        );
        assertNotNull(
            SandboxStatus.CREATED
        );
        assertNotNull(
            SandboxStatus.RUNNING
        );
        assertNotNull(
            SandboxStatus.STOPPED
        );
        assertNotNull(
            SandboxStatus.DESTROYED
        );
    }

    // ── SandboxIsolationLevel ──

    @Test
    void afWs012_fourIsolationLevels() {
        assertEquals(
            4,
            SandboxIsolationLevel.values()
                .length
        );
        assertNotNull(
            SandboxIsolationLevel.LOGICAL
        );
        assertNotNull(
            SandboxIsolationLevel.PROCESS
        );
        assertNotNull(
            SandboxIsolationLevel.CONTAINER
        );
        assertNotNull(
            SandboxIsolationLevel.VM
        );
    }

    // ── Sandbox Interface ──

    @Test
    void afWs013_sandboxIsInterface() {
        assertTrue(
            Sandbox.class.isInterface()
        );
    }

    // ── NetworkMode ──

    @Test
    void afWs014_networkMode() {
        assertTrue(
            NetworkMode.values().length
                >= 2
        );
    }

    // ── ResourceType ──

    @Test
    void afWs015_resourceType() {
        assertTrue(
            ResourceType.values().length
                >= 2
        );
    }
}
