package io.github.regalpine.agentforge.compiler;

public record CompilerOptions(
    boolean strictSchemaValidation,
    boolean deterministicOrdering,
    boolean allowExperimentalNodeTypes
) {

    public static CompilerOptions defaults() {
        return new CompilerOptions(
            true,
            true,
            false
        );
    }
}
