package io.github.regalpine.agentforge.sdk.bootstrap;

import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

/**
 * Bootstrap lifecycle state machine.
 * AF-ENHANCE-01 §19: Bootstrap lifecycle.
 *
 * <p>Manages the full lifecycle from
 * CREATE through RUNNING to STOPPED.
 * Enforces valid state transitions and
 * detects circular dependencies.
 *
 * <p>Thread-safe.
 */
public final class BootstrapLifecycle {

    private final AtomicReference<BootstrapState>
        state = new AtomicReference<>(
            BootstrapState.CREATE
        );

    private final List<Consumer<BootstrapState>>
        listeners = new ArrayList<>();

    private final DependencyGraph dependencyGraph =
        new DependencyGraph();

    /**
     * Get the current state.
     */
    public BootstrapState currentState() {
        return state.get();
    }

    /**
     * Attempt to transition to the
     * target state.
     *
     * @param target the target state
     * @throws BootstrapException if the
     *   transition is invalid
     */
    public void transition(
        BootstrapState target
    ) {
        BootstrapState current = state.get();

        if (!current.canTransitionTo(target)) {
            throw new BootstrapException(
                "Invalid transition: "
                    + current + " → " + target,
                current, target
            );
        }

        if (!state.compareAndSet(
            current, target
        )) {
            throw new BootstrapException(
                "Concurrent state modification",
                current, target
            );
        }

        notifyListeners(target);
    }

    /**
     * Register a state change listener.
     */
    public void addListener(
        Consumer<BootstrapState> listener
    ) {
        listeners.add(
            Objects.requireNonNull(
                listener, "listener"
            )
        );
    }

    /**
     * Get the dependency graph for
     * circular dependency detection.
     */
    public DependencyGraph dependencyGraph() {
        return dependencyGraph;
    }

    /**
     * Run the full bootstrap sequence
     * from CREATE to RUNNING.
     *
     * @throws BootstrapException if any
     *   phase fails
     */
    public void start() {
        transition(BootstrapState.LOAD);
        transition(BootstrapState.RESOLVE);
        transition(BootstrapState.VALIDATE);
        transition(BootstrapState.INITIALIZE);
        transition(BootstrapState.READY);
        transition(BootstrapState.RUNNING);
    }

    /**
     * Graceful shutdown.
     */
    public void stop() {
        if (state.get() == BootstrapState.RUNNING) {
            transition(BootstrapState.STOPPING);
            transition(BootstrapState.STOPPED);
        }
    }

    private void notifyListeners(
        BootstrapState newState
    ) {
        for (var listener : listeners) {
            try {
                listener.accept(newState);
            } catch (Exception ignored) {
                // Listeners must not break
                // the bootstrap flow.
            }
        }
    }
}
