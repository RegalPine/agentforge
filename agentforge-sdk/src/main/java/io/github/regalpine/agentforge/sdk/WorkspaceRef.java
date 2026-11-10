package io.github.regalpine.agentforge.sdk;

import java.util.Objects;

/**
 * Reference to a Workspace.
 * AF-ENHANCE-01 §14: AgentBuilder.workspace().
 * AF-ENHANCE-07: Workspace & Sandbox (X6 phase).
 *
 * <p>Lightweight reference that identifies
 * a Workspace by ID and scope.
 */
public record WorkspaceRef(
    String id,
    String scope
) {

    public WorkspaceRef {
        Objects.requireNonNull(id, "id");
    }

    /**
     * Create a workspace reference with
     * default scope.
     */
    public static WorkspaceRef of(String id) {
        return new WorkspaceRef(id, "EXECUTION");
    }

    /**
     * Create a workspace reference with
     * explicit scope.
     */
    public static WorkspaceRef of(
        String id, String scope
    ) {
        return new WorkspaceRef(id, scope);
    }
}
