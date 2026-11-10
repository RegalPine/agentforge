package io.github.regalpine.agentforge.compiler;

import io.github.regalpine.agentforge.graph.ir.ExecutionPlan;
import io.github.regalpine.agentforge.graph.ir.GraphIR;

import java.util.List;

public sealed interface CompilationResult
    permits CompilationResult.Success,
            CompilationResult.Failure {

    record Success(
        GraphIR graphIR,
        ExecutionPlan executionPlan
    ) implements CompilationResult {
    }

    record Failure(
        List<CompilationDiagnostic> diagnostics
    ) implements CompilationResult {

        public Failure {
            diagnostics =
                diagnostics == null
                    ? List.of()
                    : List.copyOf(diagnostics);
        }
    }
}
