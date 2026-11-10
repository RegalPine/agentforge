package io.github.regalpine.agentforge.fault;

/**
 * Exception thrown when a fault is triggered at a fault point.
 */
public class FaultException extends RuntimeException {

    private final FaultPoint faultPoint;
    private final FaultType faultType;

    public FaultException(FaultPoint faultPoint, FaultType faultType, String message) {
        super("Fault injected at " + faultPoint.id() + ": " + message);
        this.faultPoint = faultPoint;
        this.faultType = faultType;
    }

    public FaultException(FaultPoint faultPoint, FaultType faultType, String message, Throwable cause) {
        super("Fault injected at " + faultPoint.id() + ": " + message, cause);
        this.faultPoint = faultPoint;
        this.faultType = faultType;
    }

    public FaultPoint faultPoint() {
        return faultPoint;
    }

    public FaultType faultType() {
        return faultType;
    }
}
