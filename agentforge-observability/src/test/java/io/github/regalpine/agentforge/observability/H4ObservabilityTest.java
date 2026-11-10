package io.github.regalpine.agentforge.observability;


import io.github.regalpine.agentforge.observability.alert.*;
import io.github.regalpine.agentforge.observability.audit.*;
import io.github.regalpine.agentforge.observability.cost.*;
import io.github.regalpine.agentforge.observability.inspection.*;
import io.github.regalpine.agentforge.observability.metric.*;
import io.github.regalpine.agentforge.observability.slo.*;
import io.github.regalpine.agentforge.observability.trace.*;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for H4 Observability.
 */
class H4ObservabilityTest {

    // --- Gateway ---

    @Test
    void gatewayShouldEmitToAllSinks() {
        var gateway = new ObservabilityGateway();
        var sink = new CollectingSink();
        gateway.addSink(sink);

        gateway.event("test-event", Map.of());
        gateway.metric("test-metric", 42.0, Map.of());
        gateway.audit("test-audit", "t1", Map.of());

        assertEquals(3, sink.events().size());
    }

    @Test
    void gatewayShouldFilterByType() {
        var gateway = new ObservabilityGateway();
        var sink = new CollectingSink();
        gateway.addSink(sink);
        gateway.disable(SignalType.AUDIT);

        gateway.event("e", Map.of());
        gateway.audit("a", "t1", Map.of());

        assertEquals(1, sink.events().size());
        assertEquals(
            SignalType.EVENT,
            sink.events().get(0).signalType()
        );
    }

    @Test
    void gatewayShouldPauseResume() {
        var gateway = new ObservabilityGateway();
        var sink = new CollectingSink();
        gateway.addSink(sink);

        gateway.event("before", Map.of());
        gateway.pause();
        gateway.event("during", Map.of());
        gateway.resume();
        gateway.event("after", Map.of());

        assertEquals(2, sink.events().size());
    }

    // --- TraceIdentity ---

    @Test
    void traceIdentityShouldChain() {
        var root = TraceIdentity.root(
            "trace-1", "exec-1"
        );

        assertEquals("trace-1", root.traceId());
        assertEquals("exec-1", root.executionId());
        assertNull(root.parentSpanId());

        var child = root.child("span-child");
        assertEquals("trace-1", child.traceId());
        assertEquals("span-child", child.spanId());
        assertEquals(root.spanId(), child.parentSpanId());

        var node = child.forNode("node-1");
        assertEquals("node-1", node.nodeExecutionId());

        var inv = node.forInvocation("inv-1");
        assertEquals("inv-1", inv.invocationId());
    }

    // --- EnhancedTraceManager ---

    @Test
    void traceManagerShouldTrackSpans() {
        var tm = new EnhancedTraceManager();
        var exporter = new InMemorySpanExporter();
        tm.addExporter(exporter);

        String root = tm.startSpan(
            "execution", "trace-1", Map.of()
        );
        String child = tm.startChildSpan(
            "model-call", "trace-1",
            root, Map.of()
        );

        assertEquals(2, tm.activeSpanCount());

        tm.endSpan(child);
        tm.endSpan(root);

        assertEquals(0, tm.activeSpanCount());
        assertEquals(2, tm.completedSpans().size());
        assertEquals(2, exporter.count());
    }

    @Test
    void traceManagerShouldRecordErrors() {
        var tm = new EnhancedTraceManager();

        String span = tm.startSpan(
            "failing-op", "trace-1", Map.of()
        );
        tm.endSpanError(span, "Something broke");

        var spans = tm.completedSpans();
        assertEquals(1, spans.size());
        assertEquals(
            SpanData.SpanStatus.ERROR,
            spans.get(0).status()
        );
        assertEquals(
            "Something broke",
            spans.get(0).errorMessage()
        );
    }

    // --- MetricsRegistry ---

    @Test
    void metricsRegistryShouldRecordAllCategories() {
        var registry = new MetricsRegistry();

        for (MetricCategory cat :
            MetricCategory.values()
        ) {
            registry.record(
                cat, "test", 1.0, Map.of()
            );
        }

        assertEquals(12, registry.totalPoints());
        assertEquals(
            12, registry.activeCategories().size()
        );
    }

    @Test
    void metricsRegistryShouldCount() {
        var registry = new MetricsRegistry();

        registry.increment(
            MetricCategory.MODEL, "calls",
            Map.of()
        );
        registry.increment(
            MetricCategory.MODEL, "calls",
            Map.of()
        );

        assertEquals(
            2,
            registry.getCount(
                MetricCategory.MODEL, "calls"
            )
        );
    }

    @Test
    void metricExporterShouldReceive() {
        var registry = new MetricsRegistry();
        var exporter = new InMemoryMetricExporter();
        registry.addExporter(exporter);

        registry.record(
            MetricCategory.LATENCY, "p99",
            150.0, Map.of()
        );

        assertEquals(1, exporter.count());
        assertEquals(
            150.0,
            exporter.points().get(0).value()
        );
    }

    // --- AuditLogger ---

    @Test
    void auditLoggerShouldRecordEvents() {
        var logger = new AuditLogger();

        logger.log(AuditEvent.success(
            "e1", AuditAction.EXECUTION_START,
            "user-1", "t1"
        ));
        logger.log(AuditEvent.denied(
            "e2", AuditAction.CREDENTIAL_ACCESS,
            "user-2", "t1", "no permission"
        ));

        assertEquals(2, logger.count());
        assertEquals(1, logger.deniedEvents().size());
        assertEquals(
            2,
            logger.eventsForTenant("t1").size()
        );
        assertEquals(
            1,
            logger.eventsForAction(
                AuditAction.EXECUTION_START
            ).size()
        );
    }

    // --- CostCalculator ---

    @Test
    void costCalculatorShouldCompute() {
        var calc = new CostCalculator();
        calc.setPricing("gpt-4", 0.03);

        var record = calc.calculateModelCost(
            "gpt-4", "t1", 500, 500
        );

        assertEquals(0.03, record.costUsd(), 0.001);
        assertEquals(1000, record.totalTokens());

        assertEquals(0.03, calc.totalCost(), 0.001);
        assertEquals(0.03, calc.tenantCost("t1"), 0.001);
    }

    // --- DataProtection ---

    @Test
    void dataProtectionShouldHashPrompts() {
        var dp = new DataProtection();

        String hash = dp.protectPrompt("hello");
        assertNotNull(hash);
        assertNotEquals("hello", hash);
        assertEquals(64, hash.length()); // SHA-256
    }

    @Test
    void dataProtectionShouldTruncateOutput() {
        var dp = new DataProtection(true, 10);

        String result =
            dp.protectOutput("1234567890ABCDEF");
        assertTrue(result.startsWith("1234567890"));
        assertTrue(result.contains("truncated"));
    }

    @Test
    void dataProtectionShouldPassThroughWhenDisabled() {
        var dp = new DataProtection(false, 256);

        assertEquals(
            "hello", dp.protectPrompt("hello")
        );
        assertEquals(
            "output", dp.protectOutput("output")
        );
    }

    // --- SLO ---

    @Test
    void sloEvaluatorShouldDetectViolation() {
        var evaluator = new SloEvaluator();
        evaluator.addSlo(
            SloDefinition.errorRate("slo-1", 0.01)
        );

        var results = evaluator.evaluate(
            Map.of(SloMetric.ERROR_RATE, 0.05)
        );

        assertEquals(1, results.size());
        assertFalse(results.get(0).satisfied());
        assertEquals(1, evaluator.violationCount());
    }

    @Test
    void sloEvaluatorShouldPass() {
        var evaluator = new SloEvaluator();
        evaluator.addSlo(
            SloDefinition.availability(
                "slo-1", 0.999
            )
        );

        var results = evaluator.evaluate(
            Map.of(SloMetric.AVAILABILITY, 0.9999)
        );

        assertEquals(1, results.size());
        assertTrue(results.get(0).satisfied());
        assertTrue(evaluator.allSatisfied());
    }

    // --- Alert ---

    @Test
    void alertManagerShouldFire() {
        var mgr = new AlertManager();
        mgr.fire(AlertEvent.critical(
            "a1", AlertType.SLO_VIOLATION,
            "Error rate exceeded"
        ));
        mgr.fire(AlertEvent.warning(
            "a2", AlertType.LATENCY_DEGRADATION,
            "P99 latency high"
        ));

        assertEquals(2, mgr.count());
        assertEquals(1, mgr.criticalAlerts().size());
    }

    // --- Inspection ---

    @Test
    void inspectionShouldBuild() {
        var inspection = ExecutionInspection
            .builder("exec-1")
            .agentId("agent-1")
            .graphId("graph-1")
            .tenantId("t1")
            .startTime(Instant.now())
            .status(
                ExecutionInspection
                .ExecutionInspectionStatus.COMPLETED
            )
            .input(Map.of("q", "hello"))
            .output(Map.of("a", "world"))
            .build();

        assertEquals("exec-1",
            inspection.executionId());
        assertEquals("agent-1",
            inspection.agentId());
        assertTrue(inspection.isSuccess());
    }

    // --- ObsEvent factories ---

    @Test
    void obsEventShouldCreateAllTypes() {
        var event = ObsEvent.event(
            "e", Map.of()
        );
        assertEquals(
            SignalType.EVENT, event.signalType()
        );

        var trace = ObsEvent.trace(
            "t", "tr", "sp", Map.of()
        );
        assertEquals(
            SignalType.TRACE, trace.signalType()
        );

        var metric = ObsEvent.metric(
            "m", 1.0, Map.of()
        );
        assertEquals(
            SignalType.METRIC, metric.signalType()
        );

        var audit = ObsEvent.audit(
            "a", "t1", Map.of()
        );
        assertEquals(
            SignalType.AUDIT, audit.signalType()
        );
    }

    // --- Helper ---

    private static class CollectingSink
        implements ObsSink {

        private final java.util.List<ObsEvent> events =
            new java.util.ArrayList<>();

        @Override
        public void emit(ObsEvent event) {
            events.add(event);
        }

        @Override
        public String sinkName() {
            return "test-collector";
        }

        java.util.List<ObsEvent> events() {
            return events;
        }
    }
}
