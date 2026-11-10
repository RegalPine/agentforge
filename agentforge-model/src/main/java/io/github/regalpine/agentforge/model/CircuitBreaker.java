package io.github.regalpine.agentforge.model;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Circuit breaker —
 * AF-ENHANCE-09 §35.
 *
 * <p>Protects against cascading
 * failures when a model provider
 * is unhealthy.
 *
 * <p>CLOSED: normal, counts failures.
 * OPEN: blocks all requests.
 * HALF_OPEN: tests recovery.
 *
 * <p>Thread-safe via AtomicReference
 * and AtomicInteger.
 */
public final class CircuitBreaker {

    private final String name;
    private final int failureThreshold;
    private final int successThreshold;
    private final Duration openDuration;

    private final AtomicReference<
        CircuitBreakerState
    > state = new AtomicReference<>(
        CircuitBreakerState.CLOSED
    );

    private final AtomicInteger failureCount =
        new AtomicInteger(0);
    private final AtomicInteger successCount =
        new AtomicInteger(0);

    private volatile Instant openedAt;

    public CircuitBreaker(
        String name,
        int failureThreshold,
        int successThreshold,
        Duration openDuration
    ) {
        Objects.requireNonNull(
            name, "name"
        );
        Objects.requireNonNull(
            openDuration, "openDuration"
        );
        if (failureThreshold <= 0) {
            throw new IllegalArgumentException(
                "failureThreshold must be > 0"
            );
        }
        if (successThreshold <= 0) {
            throw new IllegalArgumentException(
                "successThreshold must be > 0"
            );
        }

        this.name = name;
        this.failureThreshold =
            failureThreshold;
        this.successThreshold =
            successThreshold;
        this.openDuration = openDuration;
    }

    /**
     * Default circuit breaker.
     */
    public static CircuitBreaker of(
        String name
    ) {
        return new CircuitBreaker(
            name, 5, 2,
            Duration.ofSeconds(30)
        );
    }

    /**
     * Get current state.
     */
    public CircuitBreakerState state() {
        CircuitBreakerState s =
            state.get();
        if (s == CircuitBreakerState.OPEN
            && shouldTransitionToHalfOpen()
        ) {
            transitionTo(
                CircuitBreakerState.HALF_OPEN
            );
            return CircuitBreakerState.HALF_OPEN;
        }
        return s;
    }

    /**
     * Check if requests are allowed.
     */
    public boolean allowRequest() {
        return state().allowsRequests();
    }

    /**
     * Record a success.
     */
    public void recordSuccess() {
        failureCount.set(0);
        CircuitBreakerState s = state.get();
        if (s == CircuitBreakerState.HALF_OPEN) {
            int sc = successCount.incrementAndGet();
            if (sc >= successThreshold) {
                transitionTo(
                    CircuitBreakerState.CLOSED
                );
            }
        }
    }

    /**
     * Record a failure.
     */
    public void recordFailure() {
        int fc = failureCount.incrementAndGet();
        CircuitBreakerState s = state.get();
        if (s == CircuitBreakerState.HALF_OPEN) {
            transitionTo(
                CircuitBreakerState.OPEN
            );
            return;
        }
        if (s == CircuitBreakerState.CLOSED
            && fc >= failureThreshold
        ) {
            transitionTo(
                CircuitBreakerState.OPEN
            );
        }
    }

    /**
     * Get the name.
     */
    public String name() {
        return name;
    }

    /**
     * Get failure count.
     */
    public int getFailureCount() {
        return failureCount.get();
    }

    private boolean shouldTransitionToHalfOpen() {
        Instant oa = openedAt;
        return oa != null
            && Instant.now().isAfter(
                oa.plus(openDuration)
            );
    }

    private void transitionTo(
        CircuitBreakerState target
    ) {
        state.set(target);
        if (target == CircuitBreakerState.OPEN) {
            openedAt = Instant.now();
            successCount.set(0);
        }
        if (target == CircuitBreakerState.CLOSED) {
            failureCount.set(0);
            successCount.set(0);
        }
        if (target == CircuitBreakerState.HALF_OPEN) {
            successCount.set(0);
        }
    }
}
