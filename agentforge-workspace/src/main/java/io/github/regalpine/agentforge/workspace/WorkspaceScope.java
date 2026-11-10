package io.github.regalpine.agentforge.workspace;

/**
 * Workspace scope.
 * AF-ENHANCE-07 §5: Workspace Scope.
 *
 * <p>Determines the boundary of a
 * workspace. Default is EXECUTION.
 */
public enum WorkspaceScope {

    EXECUTION,

    SESSION,

    AGENT,

    APPLICATION,

    TENANT
}
