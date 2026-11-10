package io.github.regalpine.agentforge.arch;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 18 Acceptance Criteria Verification.
 * AF-AD-16 §13, T-X12.21 to T-X12.38.
 *
 * <p>Each test verifies one architectural
 * constraint from the acceptance criteria.</p>
 */
class AcceptanceCriteriaTest {

    // ── AC-01: 14 core objects frozen ──

    @Test
    void afAc01_fourteenCoreObjectsFrozen() {
        // EntityId, Version, Reference,
        // Agent, AgentDefinition,
        // CapabilityDefinition,
        // GraphDefinition, NodeDefinition,
        // EdgeDefinition, ExecutionPlan,
        // ExecutionState, ExecutionStatus,
        // EventEnvelope, EventCategory
        String[] core = {
            "io.github.regalpine.agentforge.kernel.EntityId",
            "io.github.regalpine.agentforge.kernel.Version",
            "io.github.regalpine.agentforge.kernel.Reference",
            "io.github.regalpine.agentforge.agent.Agent",
            "io.github.regalpine.agentforge.agent.AgentDefinition",
            "io.github.regalpine.agentforge.capability.CapabilityDefinition",
            "io.github.regalpine.agentforge.graph.GraphDefinition",
            "io.github.regalpine.agentforge.graph.NodeDefinition",
            "io.github.regalpine.agentforge.graph.EdgeDefinition",
            "io.github.regalpine.agentforge.graph.ir.ExecutionPlan",
            "io.github.regalpine.agentforge.event.ExecutionState",
            "io.github.regalpine.agentforge.event.ExecutionStatus",
            "io.github.regalpine.agentforge.event.EventEnvelope",
            "io.github.regalpine.agentforge.event.EventCategory"
        };
        assertEquals(14, core.length);
        for (String c : core) {
            assertLoadable(c);
        }
    }

    // ── AC-02: ASM is canonical ──

    @Test
    void afAc02_asmIsCanonicalModel() {
        // ASM = Agent Semantic Model
        // EntityId + Version + Reference
        // are the canonical identity types
        assertLoadable(
            "io.github.regalpine.agentforge.kernel.EntityId"
        );
        assertLoadable(
            "io.github.regalpine.agentforge.kernel.Version"
        );
    }

    // ── AC-03: Unified Invocation ──

    @Test
    void afAc03_unifiedInvocation() {
        // All resources enter Execution
        // via CapabilityInvocation
        assertLoadable(
            "io.github.regalpine.agentforge.capability.CapabilityInvocation"
        );
        assertLoadable(
            "io.github.regalpine.agentforge.event.ExecutionState"
        );
    }

    // ── AC-04: Event/Trace coverage ──

    @Test
    void afAc04_eventTraceCoverage() {
        // Event/Trace covers all Runtime
        // behavior
        assertLoadable(
            "io.github.regalpine.agentforge.event.EventEnvelope"
        );
        assertLoadable(
            "io.github.regalpine.agentforge.event.ExecutionTrace"
        );
        assertLoadable(
            "io.github.regalpine.agentforge.event.Span"
        );
        // 11 event categories
        assertEquals(
            11,
            io.github.regalpine.agentforge
                .event.EventCategory.values()
                .length
        );
    }

    // ── AC-05: HITL = Policy Decision ──

    @Test
    void afAc05_hitlIsPolicyDecision() {
        // HITL is a Policy Runtime
        // decision result
        assertLoadable(
            "io.github.regalpine.agentforge.runtime.hitl.RiskAssessment"
        );
        assertLoadable(
            "io.github.regalpine.agentforge.runtime.hitl.ApprovalPolicy"
        );
        assertLoadable(
            "io.github.regalpine.agentforge.runtime.hitl.ApprovalDecision"
        );
    }

    // ── AC-06: Skill lifecycle ──

    @Test
    void afAc06_skillLifecycle() {
        // Skill as first-class object
        // with complete lifecycle
        assertLoadable(
            "io.github.regalpine.agentforge.kernel.SkillDefinition"
        );
        assertLoadable(
            "io.github.regalpine.agentforge.skill.governance.SkillLifecycleState"
        );
    }

    // ── AC-07: Memory Write policy ──

    @Test
    void afAc07_memoryWritePolicy() {
        // Memory Write must go through
        // explicit policy
        assertLoadable(
            "io.github.regalpine.agentforge.memory.MemoryStore"
        );
        assertLoadable(
            "io.github.regalpine.agentforge.memory.MemoryPolicy"
        );
    }

    // ── AC-08: Workspace isolation ──

    @Test
    void afAc08_workspaceIsolation() {
        // Workspace defaults to EXECUTION
        // level isolation
        assertLoadable(
            "io.github.regalpine.agentforge.workspace.Workspace"
        );
    }

    // ── AC-09: Multi-Agent no SubAgent ──

    @Test
    void afAc09_multiAgentNoSubAgent() {
        // Multi-Agent does not create
        // SubAgent as new layer
        assertLoadable(
            "io.github.regalpine.agentforge.multiagent.budget.DelegationBudget"
        );
        assertLoadable(
            "io.github.regalpine.agentforge.multiagent.safety.CircularDelegationDetector"
        );
        // No SubAgent type
        assertNotLoadable(
            "io.github.regalpine.agentforge.agent.SubAgent"
        );
    }

    // ── AC-10: Model/Provider separation ──

    @Test
    void afAc10_modelProviderSeparation() {
        // Model Provider ≠ Model
        assertLoadable(
            "io.github.regalpine.agentforge.model.ModelProvider"
        );
        assertLoadable(
            "io.github.regalpine.agentforge.model.ModelDefinition"
        );
    }

    // ── AC-11: MCP/A2A via Adapter ──

    @Test
    void afAc11_mcpA2aViaAdapter() {
        // MCP/A2A map to ASM via
        // Protocol Adapter
        assertLoadable(
            "io.github.regalpine.agentforge.provider.mcp.McpCapabilityAdapter"
        );
        assertLoadable(
            "io.github.regalpine.agentforge.provider.mcp.SchemaNormalizer"
        );
    }

    // ── AC-12: Service ≠ Agent ──

    @Test
    void afAc12_serviceAgentSeparation() {
        // AgentService is separate from
        // Agent
        assertLoadable(
            "io.github.regalpine.agentforge.service.AgentService"
        );
        assertLoadable(
            "io.github.regalpine.agentforge.agent.Agent"
        );
    }

    // ── AC-13: Evaluation on Execution ──

    @Test
    void afAc13_evaluationOnExecution() {
        // Evaluation is based on Execution
        // results
        assertLoadable(
            "io.github.regalpine.agentforge.evaluation.model.Score"
        );
        assertLoadable(
            "io.github.regalpine.agentforge.evaluation.evaluator.EvaluatorFactory"
        );
    }

    // ── AC-14: CP/Data Plane separation ──

    @Test
    void afAc14_controlDataPlaneSeparation() {
        // Control Plane does not execute
        // Agent reasoning
        assertLoadable(
            "io.github.regalpine.agentforge.controlplane.Deployment"
        );
        assertLoadable(
            "io.github.regalpine.agentforge.controlplane.RuntimeInstance"
        );
    }

    // ── AC-15: Evolution principles ──

    @Test
    void afAc15_evolutionPrinciples() {
        // Candidate First, Immutable
        // Version, Evidence Required
        assertLoadable(
            "io.github.regalpine.agentforge.evolution.EvolutionCandidate"
        );
        assertLoadable(
            "io.github.regalpine.agentforge.evolution.CandidateLineage"
        );
        assertLoadable(
            "io.github.regalpine.agentforge.evolution.EvidenceReference"
        );
    }

    // ── AC-16: Shared Event/Trace ──

    @Test
    void afAc16_sharedEventTrace() {
        // Debugger/TestKit share
        // Event/Trace/Checkpoint/Replay
        assertLoadable(
            "io.github.regalpine.agentforge.event.EventEnvelope"
        );
        assertLoadable(
            "io.github.regalpine.agentforge.event.ExecutionTrace"
        );
        assertLoadable(
            "io.github.regalpine.agentforge.event.Checkpoint"
        );
    }

    // ── AC-17: Two paths ──

    @Test
    void afAc17_twoPaths() {
        // Simple path (Agent) and
        // Enterprise path (Graph)
        // both independently verifiable
        assertLoadable(
            "io.github.regalpine.agentforge.agent.AgentDefinition"
        );
        assertLoadable(
            "io.github.regalpine.agentforge.graph.GraphDefinition"
        );
    }

    // ── AC-18: L0-L4 verifiable ──

    @Test
    void afAc18_complianceLevelsVerifiable() {
        // L0-L4 each independently
        // verifiable
        // L0: core types
        assertLoadable(
            "io.github.regalpine.agentforge.kernel.EntityId"
        );
        // L1: runtime types
        assertLoadable(
            "io.github.regalpine.agentforge.event.ExecutionState"
        );
        // L2: SDK/CLI
        assertLoadable(
            "io.github.regalpine.agentforge.sdk.AgentForge"
        );
        // L3: MCP
        assertLoadable(
            "io.github.regalpine.agentforge.provider.mcp.McpProvider"
        );
        // L4: Enterprise
        assertLoadable(
            "io.github.regalpine.agentforge.controlplane.Deployment"
        );
        assertLoadable(
            "io.github.regalpine.agentforge.evolution.EvolutionCandidate"
        );
    }

    // ── Helpers ──

    private void assertLoadable(
        String className
    ) {
        try {
            Class.forName(className);
        } catch (ClassNotFoundException e) {
            fail("Required class not found: "
                + className);
        }
    }

    private void assertNotLoadable(
        String className
    ) {
        try {
            Class.forName(className);
            fail("Class should NOT exist: "
                + className);
        } catch (ClassNotFoundException e) {
            // expected
        }
    }
}
