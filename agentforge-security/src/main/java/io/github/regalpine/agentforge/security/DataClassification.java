package io.github.regalpine.agentforge.security;

/**
 * Data classification levels as defined in AF-SEC-01 §43.
 * <p>
 * Used to control data flow across security boundaries.
 */
public enum DataClassification {
    PUBLIC(0),
    INTERNAL(1),
    CONFIDENTIAL(2),
    RESTRICTED(3);

    private final int sensitivity;

    DataClassification(int sensitivity) {
        this.sensitivity = sensitivity;
    }

    public int sensitivity() {
        return sensitivity;
    }

    /**
     * Check if this classification exceeds the given maximum.
     */
    public boolean exceeds(DataClassification maximum) {
        return this.sensitivity > maximum.sensitivity;
    }
}
