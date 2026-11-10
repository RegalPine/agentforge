package io.github.regalpine.agentforge.model;

import java.math.BigDecimal;
import java.util.Currency;
import java.util.Objects;

/**
 * Represents the monetary cost of a model invocation.
 * <p>
 * Costs are calculated by {@link PricingResolver} based on
 * {@link ModelUsage} and {@link ModelPricing}. Providers do not
 * determine cost; they only report usage.
 */
public record ModelCost(
    BigDecimal inputCost,
    BigDecimal outputCost,
    BigDecimal totalCost,
    Currency currency,
    String pricingModel
) {

    public ModelCost {
        Objects.requireNonNull(inputCost, "inputCost");
        Objects.requireNonNull(outputCost, "outputCost");
        Objects.requireNonNull(totalCost, "totalCost");
        Objects.requireNonNull(currency, "currency");
    }

    /**
     * Create a model cost.
     *
     * @param inputCost   cost for input tokens
     * @param outputCost  cost for output tokens
     * @param currency    the currency
     * @param pricingModel the pricing model used
     * @return a new ModelCost
     */
    public static ModelCost of(BigDecimal inputCost, BigDecimal outputCost, Currency currency, String pricingModel) {
        BigDecimal total = inputCost.add(outputCost);
        return new ModelCost(inputCost, outputCost, total, currency, pricingModel);
    }

    /**
     * Create a zero cost.
     *
     * @return a ModelCost with all zeros
     */
    public static ModelCost zero() {
        return new ModelCost(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, Currency.getInstance("USD"), "none");
    }

    /**
     * Add another ModelCost to this one.
     *
     * @param other the other ModelCost
     * @return a new ModelCost with summed values
     */
    public ModelCost add(ModelCost other) {
        Objects.requireNonNull(other, "other");
        if (!currency.equals(other.currency)) {
            throw new IllegalArgumentException("Cannot add costs in different currencies");
        }
        return new ModelCost(
            inputCost.add(other.inputCost),
            outputCost.add(other.outputCost),
            totalCost.add(other.totalCost),
            currency,
            pricingModel
        );
    }
}
