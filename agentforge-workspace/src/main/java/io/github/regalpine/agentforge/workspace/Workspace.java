package io.github.regalpine.agentforge.workspace;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

/**
 * Workspace interface.
 * AF-ENHANCE-07 §7/12: Workspace API.
 *
 * <p>A governed resource space for
 * Agent, Skill, Tool or Execution.
 *
 * <p>All access must go through
 * Workspace Runtime (Policy Check).
 */
public interface Workspace {

    /**
     * Get workspace identity.
     */
    WorkspaceId id();

    /**
     * Get workspace scope.
     */
    WorkspaceScope scope();

    /**
     * Get workspace status.
     */
    WorkspaceStatus status();

    /**
     * Get workspace policy.
     */
    WorkspacePolicy policy();

    /**
     * Check if a path exists.
     */
    boolean exists(String path);

    /**
     * Read a file as bytes.
     */
    byte[] read(String path);

    /**
     * Write bytes to a file.
     */
    void write(
        String path, byte[] data
    );

    /**
     * Delete a resource.
     */
    void delete(String path);

    /**
     * List resources at path.
     */
    List<String> list(String path);

    /**
     * Create a directory.
     */
    void createDirectory(String path);

    /**
     * Register an artifact.
     */
    void registerArtifact(Artifact artifact);

    /**
     * Get an artifact by id.
     */
    Artifact getArtifact(ArtifactId id);

    /**
     * List all artifacts.
     */
    List<Artifact> listArtifacts();

    /**
     * Get current storage usage.
     */
    long storageUsed();

    /**
     * Get current file count.
     */
    int fileCount();

    /**
     * Transition to a new status.
     */
    void transitionTo(WorkspaceStatus status);
}
