package io.github.regalpine.agentforge.workspace;

import java.util.Objects;
import java.util.Set;

/**
 * Workspace policy.
 * AF-ENHANCE-07 §20-22: Workspace Policy.
 *
 * <p>Controls: Create, Read, Write,
 * Delete, List, Execute, Upload,
 * Download, Artifact Export.
 *
 * <p>Also: Max Storage, Max File Size,
 * Max File Count, Retention,
 * Allowed/Denied Paths.
 */
public record WorkspacePolicy(
    String policyId,
    Set<WorkspacePermission> permissions,
    long maxStorageBytes,
    long maxFileSizeBytes,
    int maxFileCount,
    Set<String> allowedPaths,
    Set<String> deniedPaths
) {

    public WorkspacePolicy {
        Objects.requireNonNull(
            policyId, "policyId"
        );

        permissions =
            permissions == null
                ? Set.of(
                    WorkspacePermission.READ,
                    WorkspacePermission.WRITE
                )
                : Set.copyOf(permissions);

        if (maxStorageBytes <= 0) {
            maxStorageBytes =
                2L * 1024 * 1024 * 1024;
        }
        if (maxFileSizeBytes <= 0) {
            maxFileSizeBytes =
                100L * 1024 * 1024;
        }
        if (maxFileCount <= 0) {
            maxFileCount = 10_000;
        }

        allowedPaths =
            allowedPaths == null
                ? Set.of()
                : Set.copyOf(allowedPaths);
        deniedPaths =
            deniedPaths == null
                ? Set.of()
                : Set.copyOf(deniedPaths);
    }

    /**
     * Permissive workspace policy.
     */
    public static WorkspacePolicy permissive() {
        return new WorkspacePolicy(
            "permissive",
            Set.of(WorkspacePermission.values()),
            0, 0, 0,
            null, null
        );
    }

    /**
     * Read-only workspace policy.
     */
    public static WorkspacePolicy readOnly() {
        return new WorkspacePolicy(
            "read-only",
            Set.of(
                WorkspacePermission.READ,
                WorkspacePermission.LIST
            ),
            0, 0, 0,
            null, null
        );
    }

    /**
     * Check if a permission is granted.
     */
    public boolean hasPermission(
        WorkspacePermission perm
    ) {
        return permissions.contains(perm);
    }

    /**
     * Workspace permissions enum.
     * AF-ENHANCE-07 §21.
     */
    public enum WorkspacePermission {
        READ,
        WRITE,
        CREATE,
        DELETE,
        LIST,
        EXECUTE,
        EXPORT,
        ADMIN
    }
}
