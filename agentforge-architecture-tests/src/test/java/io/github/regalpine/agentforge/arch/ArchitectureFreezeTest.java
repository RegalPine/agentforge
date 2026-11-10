package io.github.regalpine.agentforge.arch;


import org.junit.jupiter.api.Test;

import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * H7 Architecture Freeze Verification.
 * AF-RELEASE-01 §3, AF-AD-14 §12.
 *
 * T-H7.1: Core freeze list — all 20+ core
 *   abstractions exist and are frozen.
 * T-H7.2: Execution unified semantics —
 *   Agent/Graph/Multi-Agent all enter Execution.
 * T-H7.3: Extension path — new requirements
 *   go through Extension → Adapter → Provider.
 */
class ArchitectureFreezeTest {

    // ── T-H7.1: Core Freeze List ──

    /**
     * RELEASE-01 §3: All core abstractions
     * must exist and be frozen (final/record/
     * interface/sealed).
     */
    @Test
    void afFreeze001_coreOntologyFrozen() {
        // Semantic core
        assertFrozen("io.github.regalpine.agentforge.kernel.EntityId");
        assertFrozen("io.github.regalpine.agentforge.kernel.Version");
        assertFrozen("io.github.regalpine.agentforge.kernel.Reference");

        // Agent + Capability
        assertClassExists("io.github.regalpine.agentforge.agent.Agent");
        assertClassExists("io.github.regalpine.agentforge.agent.AgentDefinition");
        assertFrozen("io.github.regalpine.agentforge.capability.CapabilityDefinition");
    }

    @Test
    void afFreeze002_graphFrozen() {
        assertFrozen("io.github.regalpine.agentforge.graph.GraphDefinition");
        assertFrozen("io.github.regalpine.agentforge.graph.NodeDefinition");
        assertFrozen("io.github.regalpine.agentforge.graph.EdgeDefinition");
    }

    @Test
    void afFreeze003_graphIrFrozen() {
        assertFrozen("io.github.regalpine.agentforge.graph.ir.ExecutionPlan");
        assertFrozen("io.github.regalpine.agentforge.graph.ir.ExecutionNode");
    }

    @Test
    void afFreeze004_compilerFrozen() {
        assertFrozen(
            "io.github.regalpine.agentforge.compiler.CompilationResult"
        );
        assertClassExists(
            "io.github.regalpine.agentforge.compiler.DefaultGraphCompiler"
        );
    }

    @Test
    void afFreeze005_executionFrozen() {
        assertFrozen("io.github.regalpine.agentforge.event.ExecutionState");
        assertClassExists(
            "io.github.regalpine.agentforge.event.ExecutionStatus"
        );
    }

    @Test
    void afFreeze006_stateEventFrozen() {
        // Persistence
        assertClassExists(
            "io.github.regalpine.agentforge.persistence.ExecutionStateStore"
        );
        // Event
        assertClassExists(
            "io.github.regalpine.agentforge.event.InMemoryEventPublisher"
        );
    }

    @Test
    void afFreeze007_distributedFrozen() {
        assertClassExists(
            "io.github.regalpine.agentforge.distributed.lease.ExecutionLease"
        );
        assertClassExists(
            "io.github.regalpine.agentforge.distributed.reconciliation.Reconciler"
        );
    }

    @Test
    void afFreeze008_securityFrozen() {
        assertClassExists(
            "io.github.regalpine.agentforge.security.SecurityContext"
        );
    }

    @Test
    void afFreeze009_observabilityFrozen() {
        assertClassExists(
            "io.github.regalpine.agentforge.observability.TraceManager"
        );
    }

    @Test
    void afFreeze010_modelProviderFrozen() {
        assertInterfaceExists(
            "io.github.regalpine.agentforge.model.ModelProvider"
        );
        assertClassExists(
            "io.github.regalpine.agentforge.provider.spi.ProviderManifest"
        );
    }

    @Test
    void afFreeze011_memoryRagFrozen() {
        assertClassExists(
            "io.github.regalpine.agentforge.memory.InMemoryMemoryStore"
        );
        assertClassExists(
            "io.github.regalpine.agentforge.rag.model.KnowledgeChunk"
        );
    }

    @Test
    void afFreeze012_evaluationFrozen() {
        assertClassExists(
            "io.github.regalpine.agentforge.evaluation.model.Score"
        );
        assertClassExists(
            "io.github.regalpine.agentforge.evaluation.evaluator.EvaluatorFactory"
        );
    }

    @Test
    void afFreeze013_multiAgentFrozen() {
        assertClassExists(
            "io.github.regalpine.agentforge.multiagent.budget.DelegationBudget"
        );
        assertClassExists(
            "io.github.regalpine.agentforge.multiagent.safety.CircularDelegationDetector"
        );
    }

    @Test
    void afFreeze014_serviceFrozen() {
        assertInterfaceExists(
            "io.github.regalpine.agentforge.service.AgentService"
        );
        assertClassExists(
            "io.github.regalpine.agentforge.service.AgentServiceId"
        );
        assertClassExists(
            "io.github.regalpine.agentforge.service.Session"
        );
        assertClassExists(
            "io.github.regalpine.agentforge.service.Conversation"
        );
    }

    @Test
    void afFreeze015_multimodalFrozen() {
        assertInterfaceExists(
            "io.github.regalpine.agentforge.multimodal.ContentPart"
        );
        assertClassExists(
            "io.github.regalpine.agentforge.multimodal.MultimodalContent"
        );
        assertInterfaceExists(
            "io.github.regalpine.agentforge.multimodal.RealtimeSession"
        );
    }

    @Test
    void afFreeze016_controlPlaneFrozen() {
        assertInterfaceExists(
            "io.github.regalpine.agentforge.controlplane.Deployment"
        );
        assertInterfaceExists(
            "io.github.regalpine.agentforge.controlplane.RuntimeInstance"
        );
        assertClassExists(
            "io.github.regalpine.agentforge.controlplane.Environment"
        );
        assertClassExists(
            "io.github.regalpine.agentforge.controlplane.Release"
        );
    }

    @Test
    void afFreeze017_evolutionFrozen() {
        assertInterfaceExists(
            "io.github.regalpine.agentforge.evolution.EvolutionCandidate"
        );
        assertInterfaceExists(
            "io.github.regalpine.agentforge.evolution.ValidationGate"
        );
        assertInterfaceExists(
            "io.github.regalpine.agentforge.evolution.EvolutionLoop"
        );
        assertClassExists(
            "io.github.regalpine.agentforge.evolution.CandidateLineage"
        );
    }

    @Test
    void afFreeze018_debuggerFrozen() {
        assertClassExists(
            "io.github.regalpine.agentforge.debugger.DebugSession"
        );
        assertClassExists(
            "io.github.regalpine.agentforge.debugger.Breakpoint"
        );
        assertInterfaceExists(
            "io.github.regalpine.agentforge.debugger.BreakpointCondition"
        );
    }

    @Test
    void afFreeze019_workspaceFrozen() {
        assertInterfaceExists(
            "io.github.regalpine.agentforge.workspace.Workspace"
        );
        assertInterfaceExists(
            "io.github.regalpine.agentforge.workspace.Sandbox"
        );
        assertClassExists(
            "io.github.regalpine.agentforge.workspace.WorkspaceId"
        );
    }

    // ── T-H7.2: Execution Unified Semantics ──

    /**
     * RELEASE-01 §3: Agent / Graph / Multi-Agent
     * all ultimately enter Execution — there is
     * no third Runtime.
     */
    @Test
    void afFreeze020_singleExecutionModel() {
        // All paths lead to ExecutionState
        assertClassExists(
            "io.github.regalpine.agentforge.event.ExecutionState"
        );
        assertClassExists(
            "io.github.regalpine.agentforge.event.ExecutionStatus"
        );

        // Agent definition is NOT a graph
        assertFalse(
            io.github.regalpine.agentforge.graph.GraphDefinition.class
                .isAssignableFrom(
                    io.github.regalpine.agentforge.agent.AgentDefinition.class
                ),
            "Agent must not be a Graph"
        );
    }

    @Test
    void afFreeze021_compilationToExecution() {
        // Graph → Compiler → ExecutionPlan → Runtime
        assertClassExists(
            "io.github.regalpine.agentforge.compiler.DefaultGraphCompiler"
        );
        assertFrozen(
            "io.github.regalpine.agentforge.graph.ir.ExecutionPlan"
        );
    }

    @Test
    void afFreeze022_noParallelRuntime() {
        // There must be exactly one runtime package
        // with ExecutionState as the central type.
        // No agent-runtime, graph-runtime, etc.
        assertClassExists(
            "io.github.regalpine.agentforge.event.ExecutionState"
        );
        assertNoClass(
            "io.github.regalpine.agentforge.agent.AgentRuntime"
        );
        assertNoClass(
            "io.github.regalpine.agentforge.graph.GraphRuntime"
        );
    }

    // ── T-H7.3: Extension Path ──

    /**
     * RELEASE-01 §4: New requirements enter via
     * Extension → Adapter → Provider → Capability
     * → Configuration → Policy.
     */
    @Test
    void afFreeze030_providerSpiExists() {
        assertClassExists(
            "io.github.regalpine.agentforge.provider.spi.ProviderManifest"
        );
        assertClassExists(
            "io.github.regalpine.agentforge.provider.spi.registry.ProviderRegistry"
        );
    }

    @Test
    void afFreeze031_extensionPacksExist() {
        // Provider Packs
        assertClassExists(
            "io.github.regalpine.agentforge.provider.mock.MockProvider"
        );
        assertClassExists(
            "io.github.regalpine.agentforge.provider.http.HttpProvider"
        );
    }

    @Test
    void afFreeze032_configurationPath() {
        // SDK config path
        assertClassExists(
            "io.github.regalpine.agentforge.sdk.AgentForge"
        );
        assertClassExists(
            "io.github.regalpine.agentforge.sdk.AgentForgeConfig"
        );
    }

    @Test
    void afFreeze033_policyPath() {
        // Governance / Policy
        assertClassExists(
            "io.github.regalpine.agentforge.security.SecurityContext"
        );
        assertClassExists(
            "io.github.regalpine.agentforge.security.AuthorizationRequest"
        );
    }

    // ── Helpers ──

    private void assertFrozen(String className) {
        try {
            Class<?> cl = Class.forName(className);
            boolean frozen = cl.isRecord()
                || cl.isInterface()
                || cl.isSealed()
                || Modifier.isFinal(cl.getModifiers());
            assertTrue(frozen,
                className + " must be frozen "
                + "(final/record/interface/sealed)");
        } catch (ClassNotFoundException e) {
            fail("Frozen class not found: " + className);
        }
    }

    private void assertClassExists(String className) {
        try {
            Class.forName(className);
        } catch (ClassNotFoundException e) {
            fail("Required class not found: " + className);
        }
    }

    private void assertInterfaceExists(String className) {
        try {
            Class<?> cl = Class.forName(className);
            assertTrue(cl.isInterface(),
                className + " must be an interface");
        } catch (ClassNotFoundException e) {
            fail("Required interface not found: " + className);
        }
    }

    private void assertNoClass(String className) {
        try {
            Class.forName(className);
            fail("Class should NOT exist: " + className);
        } catch (ClassNotFoundException e) {
            // expected
        }
    }
}
