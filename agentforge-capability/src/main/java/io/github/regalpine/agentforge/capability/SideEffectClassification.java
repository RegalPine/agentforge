package io.github.regalpine.agentforge.capability;

/**
 * Side effect classification.
 * AE-02 §45: Side Effect Classification.
 *
 * <p>Determines retry, confirmation and
 * compensation behaviour in the pipeline.
 */
public enum SideEffectClassification {

    /**
     * No state mutation (e.g. customer.query).
     * Safe to retry freely.
     */
    READ_ONLY,

    /**
     * Internal state mutation (e.g. customer.update).
     * Retry with care; may need idempotency.
     */
    WRITE,

    /**
     * External side effect (e.g. payment.execute).
     * Requires idempotency key and possibly
     * human approval before execution.
     */
    EXTERNAL_SIDE_EFFECT
}
