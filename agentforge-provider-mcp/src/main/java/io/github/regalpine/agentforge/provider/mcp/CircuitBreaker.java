package io.github.regalpine.agentforge.provider.mcp;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Circuit breaker for external capabilities.
 * AE-10 §52: Circuit Breaker.
 *
 * <p>States: CLOSED → OPEN → HALF_OPEN → CLOSED.
 * Protects against cascading failures when
 * an MCP server is consistently failing.
 */
public final class CircuitBreaker {

    public enum State {
        CLOSED,
        OPEN,
        HALF_OPEN
    }

    private final int failureThreshold;
    private final Duration openDuration;
    private final AtomicInteger failureCount;
    private final AtomicReference<State> state;
    private final AtomicReference<Instant> lastFailureTime;

    public CircuitBreaker(
        int failureThreshold,
        Duration openDuration
    ) {
        this.failureThreshold =
            failureThreshold <= 0
                ? 5
                : failureThreshold;

        this.openDuration =
            openDuration == null
                ? Duration.ofSeconds(30)
                : openDuration;

        this.failureCount = new AtomicInteger(0);
        this.state = new AtomicReference<>(State.CLOSED);
        this.lastFailureTime = new AtomicReference<>(Instant.EPOCH);
    }

    public static CircuitBreaker defaults() {
        return new CircuitBreaker(
            5, Duration.ofSeconds(30)
        );
    }

    public State state() {
        State current = state.get();

        if (current == State.OPEN) {
            Instant last = lastFailureTime.get();
            Duration elapsed =
                Duration.between(last, Instant.now());

            if (elapsed.compareTo(openDuration) >= 0) {
                state.compareAndSet(
                    State.OPEN, State.HALF_OPEN
                );

                return State.HALF_OPEN;
            }
        }

        return current;
    }

    public boolean allowRequest() {
        State s = state();
        return s == State.CLOSED || s == State.HALF_OPEN;
    }

    public void recordSuccess() {
        failureCount.set(0);
        state.set(State.CLOSED);
    }

    public void recordFailure() {
        lastFailureTime.set(Instant.now());
        int count = failureCount.incrementAndGet();

        if (count >= failureThreshold) {
            state.set(State.OPEN);
        }
    }

    public void reset() {
        failureCount.set(0);
        state.set(State.CLOSED);
        lastFailureTime.set(Instant.EPOCH);
    }
}
