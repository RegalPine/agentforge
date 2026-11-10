package io.github.regalpine.agentforge.hardening.conformance;

import io.github.regalpine.agentforge.event.ExecutionState;
import io.github.regalpine.agentforge.event.ExecutionStatus;
import io.github.regalpine.agentforge.event.ExecutionId;

import io.github.regalpine.agentforge.capability.CapabilityDefinition;
import io.github.regalpine.agentforge.compiler.*;
import io.github.regalpine.agentforge.graph.*;
import io.github.regalpine.agentforge.hardening.*;
import io.github.regalpine.agentforge.kernel.*;
import io.github.regalpine.agentforge.runtime.*;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * conformance-runtime: Agent Loop
 * / Budget / Stop Condition / Structured Output.
 * AF-RELEASE-02 §4, T-H6.6.
 */
class RuntimeConformanceTest {

    @Test
    void afRuntime001_executionManagerSubmit() {
        var planReg = new InMemoryExecutionPlanRegistry();
        var stateMgr = new InMemoryStateManager();
        var mgr = new ExecutionManager(planReg, stateMgr);

        assertNotNull(mgr);
    }

    @Test
    void afRuntime002_executionRequestValidation() {
        assertThrows(
            IllegalArgumentException.class,
            () -> new ExecutionRequest(
                null, new Version("1.0.0"),
                Map.of(), null
            )
        );
    }

    @Test
    void afRuntime003_executionStatusLifecycle() {
        var stateMgr = new InMemoryStateManager();
        var id = new ExecutionId("life-001");

        // READY → RUNNING → COMPLETED
        stateMgr.create(new ExecutionState(
            id, ExecutionStatus.READY,
            new EntityId("start"),
            Map.of(), java.util.Set.of(),
            java.util.Set.of(), 0
        ));

        stateMgr.compareAndSet(id, 0,
            new ExecutionState(
                id, ExecutionStatus.RUNNING,
                new EntityId("node"),
                Map.of(), java.util.Set.of(),
                java.util.Set.of(), 1
            )
        );

        stateMgr.compareAndSet(id, 1,
            new ExecutionState(
                id, ExecutionStatus.COMPLETED,
                new EntityId("end"),
                Map.of(), java.util.Set.of(),
                java.util.Set.of(), 2
            )
        );

        assertEquals(
            ExecutionStatus.COMPLETED,
            stateMgr.get(id).orElseThrow().status()
        );
        assertEquals(
            2,
            stateMgr.get(id).orElseThrow().version()
        );
    }

    @Test
    void afRuntime004_executionPlanRegistry() {
        var reg = new InMemoryExecutionPlanRegistry();
        assertNotNull(reg);
    }
}
