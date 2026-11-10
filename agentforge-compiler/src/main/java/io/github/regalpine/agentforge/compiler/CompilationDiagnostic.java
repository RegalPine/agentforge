package io.github.regalpine.agentforge.compiler;

import io.github.regalpine.agentforge.kernel.EntityId;

public record CompilationDiagnostic(
    String code,
    Severity severity,
    String message,
    EntityId nodeId,
    EntityId edgeId
) {

    public enum Severity {
        ERROR,
        WARNING,
        INFO
    }
}
