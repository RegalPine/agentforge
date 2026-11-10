package io.github.regalpine.agentforge.security.supply;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * SbomRegistry — AF-SEC-01 §49-50.
 *
 * Software Bill of Materials registry.
 * Tracks all components used in the system.
 *
 * Build pipeline:
 * Dependency Resolution → Vulnerability Scan →
 * License Check → SBOM → Build (§50).
 */
public final class SbomRegistry {

    private final Map<String, ComponentRecord> components
        = new ConcurrentHashMap<>();

    /**
     * Register a component.
     */
    public void register(ComponentRecord component) {
        Objects.requireNonNull(component, "component");
        components.put(component.componentId(), component);
    }

    /**
     * Get a component by ID.
     */
    public Optional<ComponentRecord> get(String componentId) {
        return Optional.ofNullable(
            components.get(componentId));
    }

    /**
     * Get all components.
     */
    public Collection<ComponentRecord> all() {
        return List.copyOf(components.values());
    }

    /**
     * Get components by type.
     */
    public List<ComponentRecord> findByType(
        ComponentRecord.ComponentType type
    ) {
        return components.values().stream()
            .filter(c -> c.type() == type)
            .toList();
    }

    /**
     * Get components by security status.
     */
    public List<ComponentRecord> findByStatus(
        ComponentRecord.SecurityStatus status
    ) {
        return components.values().stream()
            .filter(c -> c.securityStatus() == status)
            .toList();
    }

    /**
     * Check if any component is vulnerable.
     */
    public boolean hasVulnerabilities() {
        return components.values().stream()
            .anyMatch(c ->
                c.securityStatus()
                    == ComponentRecord.SecurityStatus.VULNERABLE);
    }

    /**
     * Get total component count.
     */
    public int size() {
        return components.size();
    }

    /**
     * Remove a component.
     */
    public void remove(String componentId) {
        components.remove(componentId);
    }
}
