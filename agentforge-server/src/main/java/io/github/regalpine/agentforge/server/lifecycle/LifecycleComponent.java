package io.github.regalpine.agentforge.server.lifecycle;

/**
 * Lifecycle component that can be started and stopped by the application.
 * <p>
 * Batch 4: Worker, Relay, Reconciler and connection resources implement
 * this interface for unified lifecycle management.
 */
public interface LifecycleComponent {

    /** Start the component. Called during application startup. */
    void start();

    /** Stop the component. Called during application shutdown. */
    void stop();

    /** Human-readable name for logging. */
    String name();
}
