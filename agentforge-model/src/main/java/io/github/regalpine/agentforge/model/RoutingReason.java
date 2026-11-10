package io.github.regalpine.agentforge.model;

import java.util.Objects;

/**
 * Routing reason —
 * AF-ENHANCE-09 §48.
 *
 * <p>Explains why a model was
 * selected or rejected during
 * routing.
 */
public record RoutingReason(
    String modelId,
    String providerId,
    RoutingVerdict verdict,
    String reason
) {

    public RoutingReason {
        Objects.requireNonNull(
            modelId, "modelId"
        );
        Objects.requireNonNull(
            providerId, "providerId"
        );
        Objects.requireNonNull(
            verdict, "verdict"
        );

        reason = reason == null
            ? "" : reason;
    }

    /**
     * Create a SELECTED reason.
     */
    public static RoutingReason selected(
        String modelId,
        String providerId,
        String reason
    ) {
        return new RoutingReason(
            modelId, providerId,
            RoutingVerdict.SELECTED,
            reason
        );
    }

    /**
     * Create a REJECTED reason.
     */
    public static RoutingReason rejected(
        String modelId,
        String providerId,
        String reason
    ) {
        return new RoutingReason(
            modelId, providerId,
            RoutingVerdict.REJECTED,
            reason
        );
    }

    /**
     * Routing verdict.
     */
    public enum RoutingVerdict {
        SELECTED,
        REJECTED
    }
}
