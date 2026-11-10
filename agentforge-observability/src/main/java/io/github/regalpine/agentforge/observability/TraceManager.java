package io.github.regalpine.agentforge.observability;

import java.util.Map;

public interface TraceManager {

    TraceContext start(
        String operation,
        Map<String, String> attributes
    );

    void end(
        TraceContext context
    );

    void error(
        TraceContext context,
        Throwable error
    );
}
