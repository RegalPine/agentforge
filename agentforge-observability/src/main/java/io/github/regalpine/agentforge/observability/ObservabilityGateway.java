package io.github.regalpine.agentforge.observability;

import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Observability Gateway.
 * AF-ENHANCE-06 §3: unified collection
 * point for Event/Trace/Metric/Audit
 * signals from the Runtime.
 *
 * <p>Signals are dispatched to all
 * registered sinks. Filtering by
 * signal type is supported.
 */
public final class ObservabilityGateway {

    private final List<ObsSink> sinks =
        new CopyOnWriteArrayList<>();

    private final Set<SignalType> enabled =
        EnumSet.allOf(SignalType.class);

    private final List<ObsEvent> buffer =
        Collections.synchronizedList(
            new ArrayList<>()
        );

    private volatile boolean recording = true;

    /**
     * Register a sink.
     */
    public ObservabilityGateway addSink(
        ObsSink sink
    ) {
        sinks.add(
            Objects.requireNonNull(sink, "sink")
        );
        return this;
    }

    /**
     * Enable only specific signal types.
     */
    public ObservabilityGateway enableOnly(
        SignalType... types
    ) {
        enabled.clear();
        enabled.addAll(EnumSet.copyOf(
            List.of(types)
        ));
        return this;
    }

    /**
     * Disable a signal type.
     */
    public ObservabilityGateway disable(
        SignalType type
    ) {
        enabled.remove(type);
        return this;
    }

    /**
     * Emit a signal.
     */
    public void emit(ObsEvent event) {
        if (!recording) return;
        if (!enabled.contains(event.signalType())) {
            return;
        }

        buffer.add(event);

        for (ObsSink sink : sinks) {
            try {
                sink.emit(event);
            } catch (Exception ignored) {
                // Sink errors must not
                // break the pipeline
            }
        }
    }

    /**
     * Emit an event signal.
     */
    public void event(
        String name,
        Map<String, String> attrs
    ) {
        emit(ObsEvent.event(name, attrs));
    }

    /**
     * Emit a trace signal.
     */
    public void trace(
        String name,
        String traceId,
        String spanId,
        Map<String, String> attrs
    ) {
        emit(ObsEvent.trace(
            name, traceId, spanId, attrs
        ));
    }

    /**
     * Emit a metric signal.
     */
    public void metric(
        String name,
        double value,
        Map<String, String> attrs
    ) {
        emit(ObsEvent.metric(
            name, value, attrs
        ));
    }

    /**
     * Emit an audit signal.
     */
    public void audit(
        String name,
        String tenantId,
        Map<String, String> attrs
    ) {
        emit(ObsEvent.audit(
            name, tenantId, attrs
        ));
    }

    /**
     * Pause recording.
     */
    public void pause() {
        recording = false;
    }

    /**
     * Resume recording.
     */
    public void resume() {
        recording = true;
    }

    /**
     * Check if recording is active.
     */
    public boolean isRecording() {
        return recording;
    }

    /**
     * Get buffered events (for inspection).
     */
    public List<ObsEvent> bufferedEvents() {
        return List.copyOf(buffer);
    }

    /**
     * Get the number of registered sinks.
     */
    public int sinkCount() {
        return sinks.size();
    }

    /**
     * Flush all sinks.
     */
    public void flush() {
        for (ObsSink sink : sinks) {
            sink.flush();
        }
    }

    /**
     * Close all sinks.
     */
    public void close() {
        recording = false;
        for (ObsSink sink : sinks) {
            sink.close();
        }
    }
}
