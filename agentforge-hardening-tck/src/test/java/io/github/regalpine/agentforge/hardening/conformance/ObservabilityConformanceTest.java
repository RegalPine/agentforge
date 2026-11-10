package io.github.regalpine.agentforge.hardening.conformance;

import io.github.regalpine.agentforge.hardening.*;
import io.github.regalpine.agentforge.observability.*;
import io.github.regalpine.agentforge.observability.audit.*;
import io.github.regalpine.agentforge.observability.cost.*;
import io.github.regalpine.agentforge.observability.metric.*;
import io.github.regalpine.agentforge.observability.slo.*;
import io.github.regalpine.agentforge.observability.trace.*;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * conformance-observability: Trace
 * / Metrics / Logs / Audit / Cost.
 * AF-RELEASE-02 §4, T-H6.11.
 */
class ObservabilityConformanceTest {

    @Test
    void afObs001_gatewayEmit() {
        var gw = new ObservabilityGateway();
        gw.event("test", Map.of());
    }

    @Test
    void afObs002_traceIdentity() {
        var id = TraceIdentity.root("trace-1", "exec-1");
        assertEquals("trace-1", id.traceId());
        assertEquals("exec-1", id.executionId());
        assertNotNull(id.spanId());
    }

    @Test
    void afObs003_metricCategory() {
        assertEquals(12, MetricCategory.values().length);
        assertNotNull(MetricCategory.RUNTIME);
        assertNotNull(MetricCategory.COST);
    }

    @Test
    void afObs004_auditEvent() {
        var event = AuditEvent.success(
            "evt-1", AuditAction.EXECUTION_START,
            "actor-1", "t1"
        );
        assertEquals(AuditOutcome.SUCCESS, event.outcome());
    }

    @Test
    void afObs005_costRecord() {
        var record = new CostRecord(
            "cost-1", CostLevel.MODEL,
            "entity-1", "t1", "gpt-4",
            0.05, 100, 50, null, Map.of()
        );
        assertEquals(150, record.totalTokens());
    }

    @Test
    void afObs006_sloDefinition() {
        var slo = SloDefinition.availability(
            "slo-1", 0.999
        );
        assertEquals(SloMetric.AVAILABILITY, slo.metric());
        assertEquals(0.999, slo.threshold());
    }

    @Test
    void afObs007_dataProtection() {
        var dp = new DataProtection();
        assertNotNull(dp.protectPrompt("test content"));
    }
}
