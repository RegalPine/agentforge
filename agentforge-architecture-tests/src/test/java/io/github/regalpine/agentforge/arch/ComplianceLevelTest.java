package io.github.regalpine.agentforge.arch;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * L0-L4 Compliance Level Verification.
 * AF-AD-16 §12, T-X12.16 to T-X12.20.
 *
 * <p>Five compliance levels, each
 * building on the previous:
 * <ul>
 *   <li>L0 Semantic — 14 core objects</li>
 *   <li>L1 Runtime — execution works</li>
 *   <li>L2 Developer — SDK/CLI usable</li>
 *   <li>L3 Interoperability — MCP/A2A</li>
 *   <li>L4 Enterprise — CP+Evo+TCK</li>
 * </ul>
 */
class ComplianceLevelTest {

    // ── L0: Semantic ──

    /**
     * L0: 14 core semantic objects
     * must all exist and be loadable.
     */
    @Test
    void afL0_001_coreSemanticObjects() {
        // 14 core types that must exist
        String[] coreTypes = {
            "io.github.regalpine.agentforge.kernel.EntityId",
            "io.github.regalpine.agentforge.kernel.Version",
            "io.github.regalpine.agentforge.kernel.Reference",
            "io.github.regalpine.agentforge.agent.Agent",
            "io.github.regalpine.agentforge.agent.AgentDefinition",
            "io.github.regalpine.agentforge.capability.CapabilityDefinition",
            "io.github.regalpine.agentforge.graph.GraphDefinition",
            "io.github.regalpine.agentforge.graph.NodeDefinition",
            "io.github.regalpine.agentforge.graph.ir.ExecutionPlan",
            "io.github.regalpine.agentforge.event.ExecutionState",
            "io.github.regalpine.agentforge.event.ExecutionStatus",
            "io.github.regalpine.agentforge.event.EventEnvelope",
            "io.github.regalpine.agentforge.event.EventCategory",
            "io.github.regalpine.agentforge.event.ExecutionTrace"
        };

        for (String type : coreTypes) {
            try {
                Class.forName(type);
            } catch (ClassNotFoundException e) {
                fail("L0 core type missing: " + type);
            }
        }
    }

    @Test
    void afL0_002_coreTypesAreFrozen() {
        String[] frozenTypes = {
            "io.github.regalpine.agentforge.kernel.EntityId",
            "io.github.regalpine.agentforge.kernel.Version",
            "io.github.regalpine.agentforge.graph.GraphDefinition",
            "io.github.regalpine.agentforge.graph.ir.ExecutionPlan"
        };

        for (String type : frozenTypes) {
            try {
                Class<?> cl = Class.forName(type);
                boolean frozen = cl.isRecord()
                    || cl.isInterface()
                    || cl.isSealed()
                    || java.lang.reflect.Modifier
                        .isFinal(cl.getModifiers());
                assertTrue(frozen,
                    type + " must be frozen");
            } catch (ClassNotFoundException e) {
                fail("Frozen type not found: " + type);
            }
        }
    }

    // ── L1: Runtime ──

    /**
     * L1: Core objects can execute
     * in Runtime — ExecutionState,
     * EventBus, Scheduler exist.
     */
    @Test
    void afL1_001_runtimeTypesExist() {
        String[] runtimeTypes = {
            "io.github.regalpine.agentforge.event.ExecutionState",
            "io.github.regalpine.agentforge.event.ExecutionStatus",
            "io.github.regalpine.agentforge.event.EventBus",
            "io.github.regalpine.agentforge.event.EventEnvelope",
            "io.github.regalpine.agentforge.persistence.ExecutionStateStore",
            "io.github.regalpine.agentforge.distributed.lease.ExecutionLease"
        };

        for (String type : runtimeTypes) {
            try {
                Class.forName(type);
            } catch (ClassNotFoundException e) {
                fail("L1 runtime type missing: "
                    + type);
            }
        }
    }

    @Test
    void afL1_002_executionLifecycle() {
        // ExecutionStatus has full lifecycle
        var statuses = io.github.regalpine.agentforge
            .event.ExecutionStatus.values();
        assertTrue(statuses.length >= 5,
            "Execution must have >= 5 states");
    }

    // ── L2: Developer ──

    /**
     * L2: Developer can use SDK/CLI.
     */
    @Test
    void afL2_001_sdkExists() {
        assertClassLoadable(
            "io.github.regalpine.agentforge.sdk.AgentForge"
        );
        assertClassLoadable(
            "io.github.regalpine.agentforge.sdk.AgentForgeConfig"
        );
    }

    @Test
    void afL2_002_cliExists() {
        assertClassLoadable(
            "io.github.regalpine.agentforge.cli.CliDispatcher"
        );
        assertClassLoadable(
            "io.github.regalpine.agentforge.cli.command.CreateCommand"
        );
        assertClassLoadable(
            "io.github.regalpine.agentforge.cli.command.DoctorCommand"
        );
    }

    // ── L3: Interoperability ──

    /**
     * L3: MCP/A2A protocol support.
     */
    @Test
    void afL3_001_mcpSupport() {
        assertClassLoadable(
            "io.github.regalpine.agentforge.provider.mcp.McpProvider"
        );
        assertClassLoadable(
            "io.github.regalpine.agentforge.provider.mcp.McpClientAdapter"
        );
    }

    @Test
    void afL3_002_protocolAdapter() {
        assertClassLoadable(
            "io.github.regalpine.agentforge.provider.mcp.McpCapabilityAdapter"
        );
        assertClassLoadable(
            "io.github.regalpine.agentforge.provider.mcp.SchemaNormalizer"
        );
    }

    // ── L4: Enterprise ──

    /**
     * L4: Control Plane + Evolution
     * + full TCK.
     */
    @Test
    void afL4_001_controlPlane() {
        assertClassLoadable(
            "io.github.regalpine.agentforge.controlplane.Deployment"
        );
        assertClassLoadable(
            "io.github.regalpine.agentforge.controlplane.RuntimeInstance"
        );
        assertClassLoadable(
            "io.github.regalpine.agentforge.controlplane.Environment"
        );
        assertClassLoadable(
            "io.github.regalpine.agentforge.controlplane.Release"
        );
    }

    @Test
    void afL4_002_evolution() {
        assertClassLoadable(
            "io.github.regalpine.agentforge.evolution.EvolutionCandidate"
        );
        assertClassLoadable(
            "io.github.regalpine.agentforge.evolution.ValidationGate"
        );
        assertClassLoadable(
            "io.github.regalpine.agentforge.evolution.EvolutionLoop"
        );
    }

    @Test
    void afL4_003_tckFramework() {
        assertClassLoadable(
            "io.github.regalpine.agentforge.hardening.TckRunner"
        );
        assertClassLoadable(
            "io.github.regalpine.agentforge.hardening.ReleaseGate"
        );
        assertClassLoadable(
            "io.github.regalpine.agentforge.hardening.TckReport"
        );
    }

    @Test
    void afL4_004_serviceChannel() {
        assertClassLoadable(
            "io.github.regalpine.agentforge.service.AgentService"
        );
        assertClassLoadable(
            "io.github.regalpine.agentforge.service.Session"
        );
    }

    @Test
    void afL4_005_multimodal() {
        assertClassLoadable(
            "io.github.regalpine.agentforge.multimodal.ContentPart"
        );
        assertClassLoadable(
            "io.github.regalpine.agentforge.multimodal.MultimodalContent"
        );
    }

    // ── Helpers ──

    private void assertClassLoadable(
        String className
    ) {
        try {
            Class.forName(className);
        } catch (ClassNotFoundException e) {
            fail("Required class not loadable: "
                + className);
        }
    }
}
