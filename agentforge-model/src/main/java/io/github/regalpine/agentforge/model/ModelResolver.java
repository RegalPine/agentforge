package io.github.regalpine.agentforge.model;

import java.util.Objects;

/**
 * Model resolver —
 * AF-ENHANCE-09 §22.
 *
 * <p>Resolves a ModelReference to
 * a concrete ModelDefinition via:
 *
 * <p>Reference → Profile →
 * Capability → Version →
 * Policy → Availability →
 * Resolved Model
 */
public interface ModelResolver {

    /**
     * Resolve a model reference.
     *
     * @param reference
     *     the model reference
     * @param context
     *     resolution context
     * @return
     *     the resolved definition
     * @throws ModelUnavailableException
     *     if resolution fails
     */
    ModelDefinition resolve(
        ModelReference reference,
        RoutingContext context
    );

    /**
     * Resolve by capability query.
     *
     * @param query
     *     capability query
     * @return
     *     the first matching definition
     * @throws ModelUnavailableException
     *     if no match found
     */
    ModelDefinition resolveByCapability(
        ModelCapabilityQuery query
    );
}
