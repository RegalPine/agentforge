package io.github.regalpine.agentforge.fault;

/**
 * Defines a named point in the system where faults can be injected.
 * Fault points are declared at instrumentation sites; the FaultInjector
 * decides at runtime whether to trigger a fault.
 */
public final class FaultPoint {

    private final String id;
    private final String description;

    public FaultPoint(String id, String description) {
        this.id = id;
        this.description = description;
    }

    public String id() {
        return id;
    }

    public String description() {
        return description;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof FaultPoint fp && id.equals(fp.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public String toString() {
        return "FaultPoint[" + id + "]";
    }
}
