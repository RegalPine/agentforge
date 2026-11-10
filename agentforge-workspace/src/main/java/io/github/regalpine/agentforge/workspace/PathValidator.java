package io.github.regalpine.agentforge.workspace;

import java.util.Objects;

/**
 * Path validator.
 * AF-ENHANCE-07 §10-11: Path Boundary
 * & Traversal Protection.
 *
 * <p>Ensures all paths stay within
 * the workspace root. Prevents
 * path traversal attacks (../).
 *
 * <p>Validation:
 * Normalize → Resolve → Boundary
 * Check → Policy Check → Access.
 */
public final class PathValidator {

    private final String workspaceRoot;

    public PathValidator(
        String workspaceRoot
    ) {
        Objects.requireNonNull(
            workspaceRoot, "workspaceRoot"
        );
        this.workspaceRoot =
            normalizePath(workspaceRoot);
    }

    /**
     * Validate that a path is within
     * the workspace boundary.
     *
     * @throws IllegalArgumentException
     *   if path escapes workspace
     */
    public String validate(String path) {
        Objects.requireNonNull(
            path, "path"
        );

        if (path.isBlank()) {
            throw new IllegalArgumentException(
                "Path cannot be blank"
            );
        }

        String normalized =
            normalizePath(path);

        if (!normalized.startsWith(
            workspaceRoot
        )) {
            throw new IllegalArgumentException(
                "Path escapes workspace boundary: "
                    + path
            );
        }

        return normalized;
    }

    /**
     * Check if a path is valid
     * without throwing.
     */
    public boolean isValid(String path) {
        try {
            validate(path);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Resolve a relative path against
     * the workspace root.
     */
    public String resolve(String relativePath) {
        Objects.requireNonNull(
            relativePath, "relativePath"
        );

        String cleaned = relativePath;
        if (!cleaned.startsWith("/")) {
            cleaned = "/" + cleaned;
        }

        return validate(
            workspaceRoot + cleaned
        );
    }

    /**
     * Normalize a path by resolving
     * . and .. components.
     */
    private String normalizePath(String path) {
        // Replace backslashes
        String p = path.replace('\\', '/');

        // Remove trailing slash
        while (p.length() > 1
            && p.endsWith("/")
        ) {
            p = p.substring(0, p.length() - 1);
        }

        // Split and resolve
        String[] parts = p.split("/");
        var stack =
            new java.util.ArrayDeque<String>();

        for (String part : parts) {
            if (part.isEmpty()
                || part.equals(".")
            ) {
                continue;
            }
            if (part.equals("..")) {
                if (!stack.isEmpty()) {
                    stack.removeLast();
                }
            } else {
                stack.addLast(part);
            }
        }

        StringBuilder sb = new StringBuilder();
        for (String s : stack) {
            sb.append('/').append(s);
        }

        return sb.length() == 0
            ? "/" : sb.toString();
    }

    /**
     * Get the workspace root.
     */
    public String getWorkspaceRoot() {
        return workspaceRoot;
    }
}
