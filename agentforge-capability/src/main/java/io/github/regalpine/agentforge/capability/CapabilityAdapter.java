package io.github.regalpine.agentforge.capability;

/**
 * Capability Adapter SPI.
 * AF-ENHANCE-02 §3, AF-AD-14 §6.3:
 * unified interface for all external
 * capability adapters.
 *
 * <p>The architecture enforces a strict
 * boundary:
 * <pre>
 * Capability → Provider → ToolAdapter → CapabilityAdapter → External
 * </pre>
 *
 * <p>Each adapter (HTTP, JDBC, File, MCP)
 * implements this interface. The adapter
 * is responsible for:
 * <ul>
 *   <li>Describing itself via metadata</li>
 *   <li>Executing invocations</li>
 *   <li>Reporting health</li>
 *   <li>Lifecycle management</li>
 * </ul>
 */
public interface CapabilityAdapter {

    /**
     * Adapter metadata descriptor.
     */
    AdapterMetadata metadata();

    /**
     * Execute a capability invocation
     * through this adapter.
     */
    CapabilityResult execute(
        CapabilityInvocation invocation
    );

    /**
     * Report current adapter health.
     */
    AdapterHealth health();

    /**
     * Initialise the adapter with the
     * given configuration. Called once
     * before first execution.
     */
    default void initialise(
        AdapterConfiguration configuration
    ) {
        // no-op by default
    }

    /**
     * Shut down the adapter and release
     * any resources.
     */
    default void close() {
        // no-op by default
    }
}
