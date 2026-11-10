package io.github.regalpine.agentforge.model;

import java.math.BigDecimal;
import java.util.Currency;
import java.util.Objects;

/**
 * Pricing information for a model.
 * <p>
 * Defines per-token pricing for input and output, used by
 * {@link PricingResolver} to calculate {@link ModelCost}.
 */
public record ModelPricing(
    String modelId,
    String providerId,
    BigDecimal inputTokenPrice,
    BigDecimal outputTokenPrice,
    Currency currency,
    PricingUnit unit
) {

    public ModelPricing {
        Objects.requireNonNull(modelId, "modelId");
        Objects.requireNonNull(providerId, "providerId");
        Objects.requireNonNull(inputTokenPrice, "inputTokenPrice");
        Objects.requireNonNull(outputTokenPrice, "outputTokenPrice");
        Objects.requireNonNull(currency, "currency");
        Objects.requireNonNull(unit, "unit");
    }

    /**
     * Create pricing per million tokens (standard industry unit).
     *
     * @param modelId          the model identifier
     * @param providerId       the provider identifier
     * @param inputTokenPrice  price per million input tokens
     * @param outputTokenPrice price per million output tokens
     * @param currency         the currency
     * @return a new ModelPricing
     */
    public static ModelPricing perMillionTokens(
        String modelId,
        String providerId,
        BigDecimal inputTokenPrice,
        BigDecimal outputTokenPrice,
        Currency currency
    ) {
        return new ModelPricing(modelId, providerId, inputTokenPrice, outputTokenPrice, currency, PricingUnit.PER_MILLION_TOKENS);
    }

    /**
     * Create free pricing (no cost).
     *
     * @param modelId    the model identifier
     * @param providerId the provider identifier
     * @return a ModelPricing with zero costs
     */
    public static ModelPricing free(String modelId, String providerId) {
        return new ModelPricing(
            modelId, providerId,
            BigDecimal.ZERO, BigDecimal.ZERO,
            Currency.getInstance("USD"),
            PricingUnit.PER_MILLION_TOKENS
        );
    }

    /**
     * Pricing unit for token costs.
     */
    public enum PricingUnit {
        PER_TOKEN,
        PER_THOUSAND_TOKENS,
        PER_MILLION_TOKENS
    }

    /**
     * Calculate cost for the given usage.
     *
     * @param usage the model usage
     * @return the calculated cost
     */
    public ModelCost calculate(ModelUsage usage) {
        BigDecimal divisor = switch (unit) {
            case PER_TOKEN -> BigDecimal.ONE;
            case PER_THOUSAND_TOKENS -> BigDecimal.valueOf(1_000);
            case PER_MILLION_TOKENS -> BigDecimal.valueOf(1_000_000);
        };

        BigDecimal inputCost = inputTokenPrice
            .multiply(BigDecimal.valueOf(usage.inputTokens()))
            .divide(divisor, 10, java.math.RoundingMode.HALF_UP);

        BigDecimal outputCost = outputTokenPrice
            .multiply(BigDecimal.valueOf(usage.outputTokens()))
            .divide(divisor, 10, java.math.RoundingMode.HALF_UP);

        return ModelCost.of(inputCost, outputCost, currency, providerId + ":" + modelId);
    }
}
