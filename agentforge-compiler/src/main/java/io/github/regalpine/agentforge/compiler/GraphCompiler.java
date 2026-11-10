package io.github.regalpine.agentforge.compiler;

import io.github.regalpine.agentforge.graph.GraphDefinition;

public interface GraphCompiler {

    CompilationResult compile(
        GraphDefinition graph,
        CompilationContext context
    );
}
