package io.github.regalpine.agentforge.compiler;

public record CompilationContext(
    RegistrySnapshot registrySnapshot,
    CompilerOptions options
) {

    public CompilationContext {
        if (registrySnapshot == null) {
            registrySnapshot =
                RegistrySnapshot.empty();
        }

        if (options == null) {
            options =
                CompilerOptions.defaults();
        }
    }
}
