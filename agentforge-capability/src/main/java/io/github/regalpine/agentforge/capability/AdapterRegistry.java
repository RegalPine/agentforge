package io.github.regalpine.agentforge.capability;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Adapter Registry SPI.
 * AF-ENHANCE-02 §18: provides
 * registration, discovery, health
 * checking and lifecycle management
 * for capability adapters.
 */
public interface AdapterRegistry {

    /**
     * Register an adapter.
     */
    void register(CapabilityAdapter adapter);

    /**
     * Unregister an adapter by its ID.
     */
    void unregister(String adapterId);

    /**
     * Find an adapter by its ID.
     */
    Optional<CapabilityAdapter> find(
        String adapterId
    );

    /**
     * List all registered adapters.
     */
    List<CapabilityAdapter> list();

    /**
     * Find adapters by protocol
     * (e.g. "http", "jdbc", "file", "mcp").
     */
    List<CapabilityAdapter> findByProtocol(
        String protocol
    );

    /**
     * Aggregate health summary of all
     * registered adapters.
     */
    Map<String, AdapterHealth> healthSummary();

    /**
     * Initialise all registered adapters
     * with their configurations.
     */
    void initialiseAll(
        Map<String, AdapterConfiguration> configs
    );

    /**
     * Shut down all registered adapters.
     */
    void closeAll();
}
