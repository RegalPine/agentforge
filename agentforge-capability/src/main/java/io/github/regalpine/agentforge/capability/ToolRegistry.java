package io.github.regalpine.agentforge.capability;

import java.util.Optional;

/**
 * Tool Registry SPI.
 * AF-07 §24: ToolRegistry.
 *
 * <p>Provides tool discovery capability.
 * Tools can be looked up by their identifier.
 */
public interface ToolRegistry {

    Optional<ToolDescriptor> find(
        String toolId
    );
}
