package io.github.regalpine.agentforge.fault;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Global registry of fault points and their configured rules.
 * Thread-safe. Enabled only in test environments.
 */
public final class FaultRegistry {

    private static final FaultRegistry INSTANCE = new FaultRegistry();

    private final Map<String, FaultPoint> faultPoints = new ConcurrentHashMap<>();
    private final Map<String, FaultRule> activeRules = new ConcurrentHashMap<>();
    private volatile boolean enabled = false;

    public FaultRegistry() {
    }

    public static FaultRegistry instance() {
        return INSTANCE;
    }

    public void enable() {
        this.enabled = true;
    }

    public void disable() {
        this.enabled = false;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void registerFaultPoint(FaultPoint faultPoint) {
        faultPoints.put(faultPoint.id(), faultPoint);
    }

    public FaultPoint getFaultPoint(String id) {
        return faultPoints.get(id);
    }

    public void activateRule(FaultRule rule) {
        activeRules.put(rule.faultPoint().id(), rule);
    }

    public void deactivateRule(String faultPointId) {
        activeRules.remove(faultPointId);
    }

    public void deactivateAllRules() {
        activeRules.clear();
    }

    public FaultRule getActiveRule(String faultPointId) {
        return activeRules.get(faultPointId);
    }

    public boolean hasActiveRule(String faultPointId) {
        return activeRules.containsKey(faultPointId);
    }

    public Map<String, FaultRule> allActiveRules() {
        return Map.copyOf(activeRules);
    }

    public void reset() {
        activeRules.clear();
        faultPoints.clear();
        enabled = false;
    }
}
