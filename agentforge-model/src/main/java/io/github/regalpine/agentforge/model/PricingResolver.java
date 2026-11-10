package io.github.regalpine.agentforge.model;

/**
 * Resolves model usage to monetary cost.
 * <p>
 * Pricing resolvers are responsible for looking up the appropriate
 * {@link ModelPricing} for a model and calculating the {@link ModelCost}.
 */
public interface PricingResolver {

    /**
     * Calculate the cost for a model invocation.
     *
     * @param providerId the provider identifier
     * @param modelId    the model identifier
     * @param usage      the model usage
     * @return the calculated cost
     */
    ModelCost resolve(String providerId, String modelId, ModelUsage usage);

    /**
     * Get the pricing information for a model.
     *
     * @param providerId the provider identifier
     * @param modelId    the model identifier
     * @return the pricing, or null if not known
     */
    ModelPricing pricing(String providerId, String modelId);
}
