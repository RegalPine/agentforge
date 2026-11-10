package io.github.regalpine.agentforge.fault;

import java.util.concurrent.ThreadLocalRandom;

/**
 * Main API for fault injection. Checks whether a fault should be triggered
 * at a given fault point and applies the configured fault behavior.
 */
public final class FaultInjector {

    private final FaultRegistry registry;

    public FaultInjector() {
        this.registry = FaultRegistry.instance();
    }

    public FaultInjector(FaultRegistry registry) {
        this.registry = registry;
    }

    /**
     * Check if a fault should be triggered at the given fault point.
     * Returns true if the fault was triggered and handled (exception thrown or delay applied).
     */
    public boolean check(FaultPoint faultPoint) {
        if (!registry.isEnabled()) {
            return false;
        }
        FaultRule rule = registry.getActiveRule(faultPoint.id());
        if (rule == null) {
            return false;
        }
        if (rule.probability() < 1.0 && ThreadLocalRandom.current().nextDouble() > rule.probability()) {
            return false;
        }
        return applyFault(faultPoint, rule);
    }

    /**
     * Check and throw if fault is an exception type. Convenience method.
     */
    public void checkAndThrow(FaultPoint faultPoint) {
        if (check(faultPoint)) {
            FaultRule rule = registry.getActiveRule(faultPoint.id());
            if (rule != null && rule.type() == FaultType.EXCEPTION) {
                throw new FaultException(faultPoint, rule.type(), rule.exceptionMessage());
            }
        }
    }

    private boolean applyFault(FaultPoint faultPoint, FaultRule rule) {
        switch (rule.type()) {
            case EXCEPTION:
                throw new FaultException(faultPoint, rule.type(), rule.exceptionMessage());
            case DELAY:
            case TIMEOUT:
                try {
                    Thread.sleep(rule.delay().toMillis());
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new FaultException(faultPoint, rule.type(), "Interrupted during fault delay", e);
                }
                return true;
            case FAILURE:
                throw new FaultException(faultPoint, rule.type(), rule.exceptionMessage());
            case DROP:
                throw new FaultException(faultPoint, rule.type(), "Request dropped");
            case CORRUPT:
                throw new FaultException(faultPoint, rule.type(), "Data corrupted");
            default:
                return false;
        }
    }
}
