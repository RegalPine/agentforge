package io.github.regalpine.agentforge.capability;

/**
 * Tool adapter abstraction.
 * AF-07 §26: ToolAdapter.
 *
 * <p>Intermediate layer between Provider and
 * actual Tool implementation. Provides a uniform
 * interface for different tool protocols:
 *
 * <pre>
 * Capability → Provider → ToolAdapter → JDBC/MCP/HTTP
 * </pre>
 */
public interface ToolAdapter {

    ToolDescriptor descriptor();

    CapabilityResult invoke(
        CapabilityInvocation invocation
    );
}
