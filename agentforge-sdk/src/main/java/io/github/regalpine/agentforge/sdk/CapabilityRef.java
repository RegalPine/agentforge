package io.github.regalpine.agentforge.sdk;

import java.util.Map;

/**
 * Reference to a registered Capability.
 * <p>
 * Provides invocation and description capabilities.
 * All invocations go through the existing Capability Invocation Pipeline:
 * Resolve → Validate → Authorize → Observe → Execute → Validate Result → Audit → Return.
 * <p>
 * Thread-safe.
 */
public interface CapabilityRef {

    /**
     * The unique capability identifier.
     */
    String id();

    /**
     * The capability version.
     */
    String version();

    /**
     * Get the read-only capability descriptor.
     */
    CapabilityDescriptor descriptor();

    /**
     * Invoke the capability with input.
     *
     * @param input the input data
     * @return the invocation result
     */
    InvocationResult invoke(Map<String, Object> input);
}
