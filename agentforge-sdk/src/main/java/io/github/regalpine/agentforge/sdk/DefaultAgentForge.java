package io.github.regalpine.agentforge.sdk;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

/**
 * Default implementation of AgentForge SDK.
 * <p>
 * Uses registries for agents, capabilities, and graphs.
 * Supports registration of custom implementations for embedded mode.
 */
final class DefaultAgentForge implements AgentForge {

    private final AgentForgeConfig config;
    private final AtomicReference<LifecycleStatus> lifecycle = new AtomicReference<>(LifecycleStatus.CREATED);
    private final Map<String, AgentRef> agents = new ConcurrentHashMap<>();
    private final Map<String, CapabilityRef> capabilities = new ConcurrentHashMap<>();
    private final Map<String, GraphRef> graphs = new ConcurrentHashMap<>();
    private final Map<String, ExecutionRef> executions = new ConcurrentHashMap<>();
    private final MemoryApi memoryApi = new MemoryApi();
    private final RagApi ragApi = new RagApi();
    private final EvaluationApi evaluationApi = new EvaluationApi();
    private volatile SkillApi skillApi = new NoOpSkillApi();

    DefaultAgentForge(AgentForgeConfig config) {
        this.config = Objects.requireNonNull(config, "config cannot be null");
        lifecycle.set(LifecycleStatus.READY);
    }

    /**
     * Register an agent for SDK access.
     */
    public void registerAgent(String agentId, AgentRef agentRef) {
        agents.put(agentId, agentRef);
    }

    /**
     * Register a capability for SDK access.
     */
    public void registerCapability(String capabilityId, CapabilityRef capabilityRef) {
        capabilities.put(capabilityId, capabilityRef);
    }

    /**
     * Register a graph for SDK access.
     */
    public void registerGraph(String graphId, GraphRef graphRef) {
        graphs.put(graphId, graphRef);
    }

    /**
     * Register an execution for tracking.
     */
    public void registerExecution(String executionId, ExecutionRef executionRef) {
        executions.put(executionId, executionRef);
    }

    /**
     * Register a SkillApi for SDK access.
     */
    public void registerSkillApi(SkillApi skillApi) {
        this.skillApi = Objects.requireNonNull(skillApi, "skillApi");
    }

    @Override
    public AgentRef agent(String agentId) {
        ensureRunning();
        AgentRef ref = agents.get(agentId);
        if (ref == null) {
            throw new ResolutionException("Agent", agentId);
        }
        return ref;
    }

    @Override
    public CapabilityRef capability(String capabilityId) {
        ensureRunning();
        CapabilityRef ref = capabilities.get(capabilityId);
        if (ref == null) {
            throw new ResolutionException("Capability", capabilityId);
        }
        return ref;
    }

    @Override
    public GraphRef graph(String graphId) {
        ensureRunning();
        GraphRef ref = graphs.get(graphId);
        if (ref == null) {
            throw new ResolutionException("Graph", graphId);
        }
        return ref;
    }

    @Override
    public ExecutionRef execution(String executionId) {
        ensureRunning();
        ExecutionRef ref = executions.get(executionId);
        if (ref == null) {
            throw new ResolutionException("Execution", executionId);
        }
        return ref;
    }

    @Override
    public List<String> agentIds() {
        return List.copyOf(agents.keySet());
    }

    @Override
    public List<String> capabilityIds() {
        return List.copyOf(capabilities.keySet());
    }

    @Override
    public List<String> graphIds() {
        return List.copyOf(graphs.keySet());
    }

    @Override
    public MemoryApi memory() {
        return memoryApi;
    }

    @Override
    public RagApi rag() {
        return ragApi;
    }

    @Override
    public EvaluationApi evaluation() {
        return evaluationApi;
    }

    @Override
    public SkillApi skills() {
        return skillApi;
    }

    @Override
    public AgentForgeConfig config() {
        return config;
    }

    @Override
    public LifecycleStatus lifecycleStatus() {
        return lifecycle.get();
    }

    @Override
    public void close() {
        lifecycle.set(LifecycleStatus.CLOSING);
        executions.clear();
        lifecycle.set(LifecycleStatus.CLOSED);
    }

    private void ensureRunning() {
        LifecycleStatus status = lifecycle.get();
        if (status == LifecycleStatus.CLOSED || status == LifecycleStatus.CLOSING) {
            throw new AgentForgeException("SDK_CLOSED", "AgentForge SDK is already closed");
        }
        if (status == LifecycleStatus.CREATED) {
            lifecycle.compareAndSet(LifecycleStatus.CREATED, LifecycleStatus.RUNNING);
        }
    }
}
