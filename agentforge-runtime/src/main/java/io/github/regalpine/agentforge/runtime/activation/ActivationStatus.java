package io.github.regalpine.agentforge.runtime.activation;

/**
 * Status of an activation lifecycle.
 */
public enum ActivationStatus {
    /** Activation created, branches not yet started */
    PENDING,
    /** All branches are executing */
    ACTIVE,
    /** All branches completed successfully */
    COMPLETED,
    /** One or more branches failed */
    FAILED,
    /** Activation was cancelled */
    CANCELLED
}
