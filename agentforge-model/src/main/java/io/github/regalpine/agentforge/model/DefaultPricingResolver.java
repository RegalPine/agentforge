package io.github.regalpine.agentforge.model;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Default in-memory pricing resolver that stores pricing information
 * for known models and resolves costs based on usage.
 */
public class DefaultPricingResolver implements PricingResolver {

    private final Map<String, ModelPricing> pricingMap = new ConcurrentHashMap<>();

    /**
     * Register pricing for a model.
     *
     * @param pricing the model pricing
     */
    public void register(ModelPricing pricing) {
        pricingMap.put(compositeKey(pricing.providerId(), pricing.modelId()), pricing);
    }

    @Override
    public ModelCost resolve(String providerId, String modelId, ModelUsage usage) {
        ModelPricing pricing = pricing(providerId, modelId);
        if (pricing == null) {
            return ModelCost.zero();
        }
        return pricing.calculate(usage);
    }

    @Override
    public ModelPricing pricing(String providerId, String modelId) {
        return pricingMap.get(compositeKey(providerId, modelId));
    }

    private static String compositeKey(String providerId, String modelId) {
        return providerId + ":" + modelId;
    }
}
