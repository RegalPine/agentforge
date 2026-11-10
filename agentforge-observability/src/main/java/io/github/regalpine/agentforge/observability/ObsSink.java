package io.github.regalpine.agentforge.observability;

/**
 * Observability signal sink.
 * AF-ENHANCE-06 §3: the gateway
 * dispatches collected signals to
 * one or more sinks.
 */
public interface ObsSink {

    /**
     * Accept a signal.
     */
    void emit(ObsEvent event);

    /**
     * Flush buffered signals.
     */
    default void flush() {}

    /**
     * Close and release resources.
     */
    default void close() {}

    /**
     * Sink name for diagnostics.
     */
    String sinkName();
}
