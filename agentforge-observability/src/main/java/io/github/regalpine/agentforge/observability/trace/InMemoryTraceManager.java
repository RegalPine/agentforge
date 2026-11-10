package io.github.regalpine.agentforge.observability.trace;

import io.github.regalpine.agentforge.observability.TraceContext;
import io.github.regalpine.agentforge.observability.TraceManager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

public final class InMemoryTraceManager
    implements TraceManager {

    private final List<TraceRecord> records =
        Collections.synchronizedList(new ArrayList<>());

    private final AtomicLong spanCounter =
        new AtomicLong(0);

    @Override
    public TraceContext start(
        String operation,
        Map<String, String> attributes
    ) {
        String spanId =
            "span-" + spanCounter.incrementAndGet();

        String traceId =
            UUID.randomUUID().toString();

        TraceContext ctx =
            new TraceContext(
                traceId,
                spanId,
                null,
                operation
            );

        records.add(
            new TraceRecord(
                ctx,
                "START",
                null
            )
        );

        return ctx;
    }

    @Override
    public void end(
        TraceContext context
    ) {
        records.add(
            new TraceRecord(
                context,
                "END",
                null
            )
        );
    }

    @Override
    public void error(
        TraceContext context,
        Throwable error
    ) {
        records.add(
            new TraceRecord(
                context,
                "ERROR",
                error.getMessage()
            )
        );
    }

    public List<TraceRecord> records() {
        return List.copyOf(records);
    }

    public record TraceRecord(
        TraceContext context,
        String status,
        String errorMessage
    ) {
    }
}
