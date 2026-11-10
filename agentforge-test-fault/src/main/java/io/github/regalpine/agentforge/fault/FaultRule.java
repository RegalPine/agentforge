package io.github.regalpine.agentforge.fault;

import java.time.Duration;
import java.util.Objects;

/**
 * A rule that configures how a fault is injected at a specific fault point.
 */
public final class FaultRule {

    private final FaultPoint faultPoint;
    private final FaultType type;
    private final Duration delay;
    private final String exceptionMessage;
    private final double probability;

    private FaultRule(Builder builder) {
        this.faultPoint = builder.faultPoint;
        this.type = builder.type;
        this.delay = builder.delay;
        this.exceptionMessage = builder.exceptionMessage;
        this.probability = builder.probability;
    }

    public FaultPoint faultPoint() {
        return faultPoint;
    }

    public FaultType type() {
        return type;
    }

    public Duration delay() {
        return delay;
    }

    public String exceptionMessage() {
        return exceptionMessage;
    }

    public double probability() {
        return probability;
    }

    public static Builder builder(FaultPoint faultPoint) {
        return new Builder(faultPoint);
    }

    public static class Builder {
        private final FaultPoint faultPoint;
        private FaultType type = FaultType.EXCEPTION;
        private Duration delay = Duration.ofSeconds(5);
        private String exceptionMessage = "Injected fault";
        private double probability = 1.0;

        private Builder(FaultPoint faultPoint) {
            this.faultPoint = Objects.requireNonNull(faultPoint);
        }

        public Builder type(FaultType type) {
            this.type = type;
            return this;
        }

        public Builder delay(Duration delay) {
            this.delay = delay;
            return this;
        }

        public Builder exceptionMessage(String message) {
            this.exceptionMessage = message;
            return this;
        }

        public Builder probability(double probability) {
            if (probability < 0.0 || probability > 1.0) {
                throw new IllegalArgumentException("probability must be in [0.0, 1.0]");
            }
            this.probability = probability;
            return this;
        }

        public FaultRule build() {
            return new FaultRule(this);
        }
    }
}
