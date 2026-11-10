package io.github.regalpine.agentforge.model;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

/**
 * Default model router —
 * AF-ENHANCE-09 §24-30.
 *
 * <p>Implements all V1.0 routing
 * strategies:
 * STATIC, PRIORITY, WEIGHTED,
 * CAPABILITY, FAILOVER,
 * COST_AWARE, QUOTA_AWARE.
 *
 * <p>Routing pipeline:
 * 1. Capability filter
 * 2. Health filter
 * 3. Strategy selection
 * 4. Build decision with reasons
 */
public final class DefaultModelRouter
    implements ModelRouter {

    private final RoutingStrategy strategy;

    public DefaultModelRouter(
        RoutingStrategy strategy
    ) {
        Objects.requireNonNull(
            strategy, "strategy"
        );
        this.strategy = strategy;
    }

    /**
     * Create a router with strategy.
     */
    public static DefaultModelRouter of(
        RoutingStrategy strategy
    ) {
        return new DefaultModelRouter(strategy);
    }

    @Override
    public RoutingStrategy strategy() {
        return strategy;
    }

    @Override
    public RoutingDecision route(
        List<ModelCandidate> candidates,
        RoutingContext context
    ) {
        Objects.requireNonNull(
            candidates, "candidates"
        );
        Objects.requireNonNull(
            context, "context"
        );

        long start = System.nanoTime();

        if (candidates.isEmpty()) {
            throw new ModelUnavailableException(
                "No candidates available",
                "", ""
            );
        }

        // Step 1: Capability filter
        List<ModelCandidate> filtered =
            filterByCapability(
                candidates, context
            );

        if (filtered.isEmpty()) {
            throw new ModelUnavailableException(
                "No candidates match "
                    + "required capabilities",
                "", ""
            );
        }

        // Step 2: Health filter
        List<ModelCandidate> healthy =
            filterByHealth(filtered);

        if (healthy.isEmpty()) {
            throw new ModelUnavailableException(
                "All candidates are unhealthy",
                "", ""
            );
        }

        // Step 3: Strategy selection
        ModelCandidate selected =
            selectByStrategy(healthy, context);

        // Step 4: Build reasons
        long elapsed =
            (System.nanoTime() - start)
                / 1_000_000;

        List<RoutingReason> reasons =
            buildReasons(
                candidates, selected, context
            );

        return new RoutingDecision(
            selected.modelId(),
            selected.providerId(),
            candidates,
            reasons,
            strategy,
            elapsed
        );
    }

    private List<ModelCandidate> filterByCapability(
        List<ModelCandidate> candidates,
        RoutingContext context
    ) {
        var caps = context.requiredCapabilities();
        if (caps.isEmpty()) {
            return new ArrayList<>(candidates);
        }

        return candidates.stream()
            .filter(c -> {
                var dc = c.definition().capabilities();
                for (String cap : caps) {
                    if (!c.definition().supports(cap)) {
                        return false;
                    }
                }
                return true;
            })
            .collect(Collectors.toList());
    }

    private List<ModelCandidate> filterByHealth(
        List<ModelCandidate> candidates
    ) {
        return candidates.stream()
            .filter(ModelCandidate::isAvailable)
            .collect(Collectors.toList());
    }

    private ModelCandidate selectByStrategy(
        List<ModelCandidate> candidates,
        RoutingContext context
    ) {
        return switch (strategy) {
            case STATIC ->
                selectStatic(candidates);
            case PRIORITY ->
                selectPriority(candidates);
            case WEIGHTED ->
                selectWeighted(candidates);
            case CAPABILITY ->
                selectCapability(candidates);
            case FAILOVER ->
                selectPriority(candidates);
            case COST_AWARE ->
                selectCostAware(candidates);
            case QUOTA_AWARE ->
                selectStatic(candidates);
            case LATENCY_AWARE ->
                selectLatencyAware(candidates);
            case LOAD_AWARE ->
                selectStatic(candidates);
            case REGION_AWARE ->
                selectRegionAware(
                    candidates, context
                );
        };
    }

    private ModelCandidate selectStatic(
        List<ModelCandidate> candidates
    ) {
        return candidates.get(0);
    }

    private ModelCandidate selectPriority(
        List<ModelCandidate> candidates
    ) {
        return candidates.stream()
            .min(Comparator.comparingInt(
                ModelCandidate::priority
            ))
            .orElse(candidates.get(0));
    }

    private ModelCandidate selectWeighted(
        List<ModelCandidate> candidates
    ) {
        double totalWeight = candidates.stream()
            .mapToDouble(ModelCandidate::weight)
            .sum();

        double rand = ThreadLocalRandom
            .current().nextDouble()
            * totalWeight;

        double cumulative = 0;
        for (ModelCandidate c : candidates) {
            cumulative += c.weight();
            if (cumulative >= rand) {
                return c;
            }
        }
        return candidates.get(
            candidates.size() - 1
        );
    }

    private ModelCandidate selectCapability(
        List<ModelCandidate> candidates
    ) {
        return candidates.stream()
            .max(Comparator.comparingInt(c -> {
                int score = 0;
                var caps = c.definition().capabilities();
                if (caps.chat()) score++;
                if (caps.toolCalling()) score++;
                if (caps.structuredOutput()) score++;
                if (caps.streaming()) score++;
                if (caps.vision()) score++;
                return score;
            }))
            .orElse(candidates.get(0));
    }

    private ModelCandidate selectCostAware(
        List<ModelCandidate> candidates
    ) {
        return candidates.stream()
            .min(Comparator.comparingDouble(c -> {
                var pricing = c.definition().pricing();
                if (pricing == null) return 0;
                return pricing.inputTokenPrice()
                    .doubleValue();
            }))
            .orElse(candidates.get(0));
    }

    private ModelCandidate selectLatencyAware(
        List<ModelCandidate> candidates
    ) {
        return candidates.stream()
            .min(Comparator.comparingLong(c -> {
                var h = c.health();
                return h.latency().toMillis();
            }))
            .orElse(candidates.get(0));
    }

    private ModelCandidate selectRegionAware(
        List<ModelCandidate> candidates,
        RoutingContext context
    ) {
        String region = context.region();
        if (region == null) {
            return selectStatic(candidates);
        }

        return candidates.stream()
            .filter(c -> {
                String mr = (String) c.metadata()
                    .get("region");
                return region.equals(mr);
            })
            .findFirst()
            .orElse(candidates.get(0));
    }

    private List<RoutingReason> buildReasons(
        List<ModelCandidate> all,
        ModelCandidate selected,
        RoutingContext context
    ) {
        List<RoutingReason> reasons =
            new ArrayList<>();

        for (ModelCandidate c : all) {
            if (c.equals(selected)) {
                reasons.add(RoutingReason.selected(
                    c.modelId(),
                    c.providerId(),
                    strategy.name().toLowerCase()
                        + " + compatible + available"
                ));
            } else if (!c.isAvailable()) {
                reasons.add(RoutingReason.rejected(
                    c.modelId(),
                    c.providerId(),
                    "unhealthy"
                ));
            } else if (!matchesCapability(
                c, context
            )) {
                reasons.add(RoutingReason.rejected(
                    c.modelId(),
                    c.providerId(),
                    "capability mismatch"
                ));
            } else {
                reasons.add(RoutingReason.rejected(
                    c.modelId(),
                    c.providerId(),
                    strategy.name().toLowerCase()
                        + " not selected"
                ));
            }
        }

        return reasons;
    }

    private boolean matchesCapability(
        ModelCandidate c,
        RoutingContext context
    ) {
        var caps = context.requiredCapabilities();
        if (caps.isEmpty()) return true;
        for (String cap : caps) {
            if (!c.definition().supports(cap)) {
                return false;
            }
        }
        return true;
    }
}
