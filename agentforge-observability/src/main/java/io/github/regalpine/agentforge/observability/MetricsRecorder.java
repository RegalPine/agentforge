package io.github.regalpine.agentforge.observability;

import java.util.Map;

public interface MetricsRecorder {

    void increment(
        String name,
        Map<String, String> attributes
    );

    void record(
        String name,
        double value,
        Map<String, String> attributes
    );
}
