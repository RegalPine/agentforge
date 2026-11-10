package io.github.regalpine.agentforge.observability;

import io.github.regalpine.agentforge.observability.metric.InMemoryMetricsRecorder;
import io.github.regalpine.agentforge.observability.trace.InMemoryTraceManager;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ObservabilityModuleTest {

    @Test
    void shouldIncrementCounter() {
        var recorder =
            new InMemoryMetricsRecorder();

        recorder.increment(
            "agentforge.executions.total",
            Map.of("status", "completed")
        );

        recorder.increment(
            "agentforge.executions.total",
            Map.of("status", "completed")
        );

        assertEquals(
            2,
            recorder.getCount(
                "agentforge.executions.total"
            )
        );
    }

    @Test
    void shouldRecordValue() {
        var recorder =
            new InMemoryMetricsRecorder();

        recorder.record(
            "agentforge.executions.duration",
            230.5,
            Map.of("graph", "test")
        );

        assertEquals(1, recorder.recordedValues().size());
        assertEquals(
            230.5,
            recorder.recordedValues().get(0).value()
        );
    }

    @Test
    void shouldReturnZeroForUnknownCounter() {
        var recorder =
            new InMemoryMetricsRecorder();

        assertEquals(
            0,
            recorder.getCount("unknown")
        );
    }

    @Test
    void shouldStartAndEndTrace() {
        var tracer =
            new InMemoryTraceManager();

        TraceContext ctx =
            tracer.start(
                "execution.run",
                Map.of("graphId", "g1")
            );

        assertNotNull(ctx.traceId());
        assertNotNull(ctx.spanId());
        assertEquals(
            "execution.run",
            ctx.operation()
        );

        tracer.end(ctx);

        assertEquals(2, tracer.records().size());
        assertEquals(
            "START",
            tracer.records().get(0).status()
        );
        assertEquals(
            "END",
            tracer.records().get(1).status()
        );
    }

    @Test
    void shouldRecordTraceError() {
        var tracer =
            new InMemoryTraceManager();

        TraceContext ctx =
            tracer.start(
                "node.execute",
                Map.of()
            );

        tracer.error(
            ctx,
            new RuntimeException("provider failed")
        );

        assertEquals(2, tracer.records().size());
        assertEquals(
            "ERROR",
            tracer.records().get(1).status()
        );
        assertEquals(
            "provider failed",
            tracer.records().get(1).errorMessage()
        );
    }

    @Test
    void shouldCreateCorrelationContext() {
        var ctx =
            new CorrelationContext(
                "req-001",
                "trace-001",
                "exec-001",
                "node-calc",
                "inv-001",
                "task-001",
                "worker-1",
                "provider-openai",
                "tenant-a"
            );

        assertEquals(
            "req-001",
            ctx.requestId()
        );
        assertEquals(
            "tenant-a",
            ctx.tenantId()
        );
    }

    @Test
    void shouldCreateDiagnosticError() {
        var error =
            new DiagnosticError(
                "AF-R001",
                FailureCategory.CAPABILITY.name(),
                "Provider returned failure",
                "CapabilityExecutor",
                "exec-001",
                "node-calc",
                "inv-001",
                true
            );

        assertEquals(
            "AF-R001",
            error.code()
        );
        assertTrue(error.retryable());
        assertEquals(
            "CAPABILITY",
            error.category()
        );
    }

    @Test
    void shouldHaveAllFailureCategories() {
        assertEquals(
            18,
            FailureCategory.values().length
        );

        assertNotNull(
            FailureCategory.valueOf("FENCING")
        );
        assertNotNull(
            FailureCategory.valueOf("STATE_CONFLICT")
        );
    }
}
