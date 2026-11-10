package io.github.regalpine.agentforge.workspace;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * In-memory workspace.
 * AF-ENHANCE-07 §77: Local Workspace.
 *
 * <p>In-memory implementation for
 * development and testing. Uses
 * ConcurrentHashMap for thread safety.
 *
 * <p>Enforces:
 * - Path boundary (PathValidator)
 * - Policy (WorkspacePolicy)
 * - Quota (maxStorage, maxFileCount)
 * - Lifecycle (WorkspaceStatus)
 */
public final class InMemoryWorkspace
    implements Workspace {

    private final WorkspaceId id;
    private final WorkspaceScope scope;
    private final WorkspacePolicy policy;
    private final PathValidator validator;

    private volatile WorkspaceStatus status;

    private final ConcurrentHashMap<
        String, byte[]
    > files = new ConcurrentHashMap<>();

    private final ConcurrentHashMap<
        String, Boolean
    > directories = new ConcurrentHashMap<>();

    private final ConcurrentHashMap<
        String, Artifact
    > artifacts = new ConcurrentHashMap<>();

    public InMemoryWorkspace(
        WorkspaceId id,
        WorkspaceScope scope,
        WorkspacePolicy policy
    ) {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(scope, "scope");
        Objects.requireNonNull(policy, "policy");

        this.id = id;
        this.scope = scope;
        this.policy = policy;
        this.status = WorkspaceStatus.CREATED;
        this.validator = new PathValidator(
            "/workspace/" + id.value()
        );
    }

    @Override
    public WorkspaceId id() {
        return id;
    }

    @Override
    public WorkspaceScope scope() {
        return scope;
    }

    @Override
    public WorkspaceStatus status() {
        return status;
    }

    @Override
    public WorkspacePolicy policy() {
        return policy;
    }

    @Override
    public boolean exists(String path) {
        checkAccessible();
        String p = validator.resolve(path);
        return files.containsKey(p)
            || directories.containsKey(p);
    }

    @Override
    public byte[] read(String path) {
        checkAccessible();
        checkPermission(
            WorkspacePolicy
                .WorkspacePermission.READ
        );

        String p = validator.resolve(path);

        if (directories.containsKey(p)) {
            throw new IllegalArgumentException(
                "Cannot read directory: " + path
            );
        }

        byte[] data = files.get(p);
        if (data == null) {
            throw new IllegalArgumentException(
                "File not found: " + path
            );
        }
        return data.clone();
    }

    @Override
    public void write(
        String path, byte[] data
    ) {
        checkAccessible();
        checkPermission(
            WorkspacePolicy
                .WorkspacePermission.WRITE
        );
        Objects.requireNonNull(data, "data");

        String p = validator.resolve(path);

        // Check file size limit
        if (data.length
            > policy.maxFileSizeBytes()
        ) {
            throw new IllegalStateException(
                "File exceeds max size: "
                    + data.length
                    + " > "
                    + policy.maxFileSizeBytes()
            );
        }

        // Check storage quota
        long currentUsed = storageUsed();
        long existing = 0;
        byte[] old = files.get(p);
        if (old != null) {
            existing = old.length;
        }
        if (currentUsed - existing
            + data.length
            > policy.maxStorageBytes()
        ) {
            throw new IllegalStateException(
                "Storage quota exceeded"
            );
        }

        // Check file count
        if (!files.containsKey(p)
            && files.size()
                >= policy.maxFileCount()
        ) {
            throw new IllegalStateException(
                "File count limit: "
                    + policy.maxFileCount()
            );
        }

        files.put(p, data.clone());
    }

    @Override
    public void delete(String path) {
        checkAccessible();
        checkPermission(
            WorkspacePolicy
                .WorkspacePermission.DELETE
        );

        String p = validator.resolve(path);

        boolean removedFile =
            files.remove(p) != null;
        boolean removedDir =
            directories.remove(p) != null;

        if (!removedFile && !removedDir) {
            // Also try prefix delete
            // for directories
            List<String> children =
                files.keySet().stream()
                    .filter(
                        k -> k.startsWith(p + "/")
                    )
                    .collect(
                        Collectors.toList()
                    );
            for (String child : children) {
                files.remove(child);
            }
            if (children.isEmpty()) {
                throw new IllegalArgumentException(
                    "Not found: " + path
                );
            }
        }
    }

    @Override
    public List<String> list(String path) {
        checkAccessible();
        checkPermission(
            WorkspacePolicy
                .WorkspacePermission.LIST
        );

        String p = validator.resolve(path);
        String prefix =
            p.endsWith("/") ? p : p + "/";

        List<String> result = new ArrayList<>();

        // Direct children from files
        for (String key : files.keySet()) {
            if (key.startsWith(prefix)) {
                String rest = key.substring(
                    prefix.length()
                );
                int slash = rest.indexOf('/');
                String child = slash < 0
                    ? rest
                    : rest.substring(0, slash);
                if (!child.isEmpty()
                    && !result.contains(child)
                ) {
                    result.add(child);
                }
            }
        }

        // Direct children from dirs
        for (String key : directories.keySet()) {
            if (key.startsWith(prefix)) {
                String rest = key.substring(
                    prefix.length()
                );
                int slash = rest.indexOf('/');
                String child = slash < 0
                    ? rest
                    : rest.substring(0, slash);
                if (!child.isEmpty()
                    && !result.contains(child)
                ) {
                    result.add(child);
                }
            }
        }

        return List.copyOf(result);
    }

    @Override
    public void createDirectory(String path) {
        checkAccessible();
        checkPermission(
            WorkspacePolicy
                .WorkspacePermission.CREATE
        );

        String p = validator.resolve(path);
        directories.put(p, Boolean.TRUE);
    }

    @Override
    public void registerArtifact(
        Artifact artifact
    ) {
        Objects.requireNonNull(
            artifact, "artifact"
        );
        artifacts.put(
            artifact.artifactId().value(),
            artifact
        );
    }

    @Override
    public Artifact getArtifact(
        ArtifactId id
    ) {
        Objects.requireNonNull(id, "id");
        return artifacts.get(id.value());
    }

    @Override
    public List<Artifact> listArtifacts() {
        return List.copyOf(
            artifacts.values()
        );
    }

    @Override
    public long storageUsed() {
        long total = 0;
        for (byte[] data : files.values()) {
            total += data.length;
        }
        return total;
    }

    @Override
    public int fileCount() {
        return files.size();
    }

    @Override
    public void transitionTo(
        WorkspaceStatus target
    ) {
        Objects.requireNonNull(
            target, "target"
        );
        this.status = target;
    }

    // -- internal helpers --

    private void checkAccessible() {
        if (!status.isAccessible()) {
            throw new IllegalStateException(
                "Workspace not accessible: "
                    + status
            );
        }
    }

    private void checkPermission(
        WorkspacePolicy.WorkspacePermission perm
    ) {
        if (!policy.hasPermission(perm)) {
            throw new SecurityException(
                "Permission denied: " + perm
            );
        }
    }

    /**
     * Create a workspace builder.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for InMemoryWorkspace.
     */
    public static final class Builder {

        private String id;
        private WorkspaceScope scope =
            WorkspaceScope.EXECUTION;
        private WorkspacePolicy policy;
        private final Map<String, String>
            seedFiles = new LinkedHashMap<>();

        private Builder() {}

        public Builder id(String id) {
            this.id = id;
            return this;
        }

        public Builder scope(
            WorkspaceScope scope
        ) {
            this.scope = scope;
            return this;
        }

        public Builder policy(
            WorkspacePolicy policy
        ) {
            this.policy = policy;
            return this;
        }

        /**
         * Add a seed file.
         */
        public Builder file(
            String path, String content
        ) {
            seedFiles.put(path, content);
            return this;
        }

        public InMemoryWorkspace build() {
            if (id == null || id.isBlank()) {
                throw new IllegalArgumentException(
                    "Workspace id required"
                );
            }

            WorkspacePolicy p =
                policy == null
                    ? WorkspacePolicy.permissive()
                    : policy;

            InMemoryWorkspace ws =
                new InMemoryWorkspace(
                    new WorkspaceId(id),
                    scope, p
                );

            // Transition to READY
            // before seeding
            ws.transitionTo(
                WorkspaceStatus.READY
            );

            // Seed files
            for (var entry
                : seedFiles.entrySet()
            ) {
                ws.write(
                    entry.getKey(),
                    entry.getValue().getBytes(
                        StandardCharsets.UTF_8
                    )
                );
            }

            return ws;
        }
    }
}
