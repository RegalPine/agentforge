package io.github.regalpine.agentforge.sdk;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;

import static org.junit.jupiter.api.Assertions.*;

class AgentForgeSdkTest {

    // --- AgentForgeConfig ---

    @Test
    void shouldCreateDefaultConfig() {
        AgentForgeConfig config = AgentForgeConfig.defaults();
        assertEquals("default", config.applicationId());
        assertEquals("default", config.tenantId());
        assertTrue(config.modelConfig().isEmpty());
    }

    @Test
    void shouldBuildConfig() {
        AgentForgeConfig config = AgentForgeConfig.builder()
            .applicationId("my-app")
            .tenantId("tenant-1")
            .modelConfig(Map.of("provider", "openai"))
            .build();
        assertEquals("my-app", config.applicationId());
        assertEquals("tenant-1", config.tenantId());
        assertEquals("openai", config.modelConfig().get("provider"));
    }

    // --- AgentForge Lifecycle ---

    @Test
    void shouldBootstrapAndClose() {
        AgentForge forge = AgentForge.bootstrap();
        assertEquals(AgentForge.LifecycleStatus.READY, forge.lifecycleStatus());
        forge.close();
        assertEquals(AgentForge.LifecycleStatus.CLOSED, forge.lifecycleStatus());
    }

    @Test
    void shouldThrowWhenAccessingAfterClose() {
        AgentForge forge = AgentForge.bootstrap();
        forge.close();
        assertThrows(AgentForgeException.class, () -> forge.agent("test"));
    }

    // --- Agent Resolution ---

    @Test
    void shouldResolveRegisteredAgent() {
        DefaultAgentForge forge = (DefaultAgentForge) AgentForge.bootstrap();
        forge.registerAgent("test-agent", new FunctionalAgentRef("test-agent", "1.0.0",
            input -> ExecutionResult.success("exec-1", Map.of("answer", "hello"))));

        AgentRef ref = forge.agent("test-agent");
        assertEquals("test-agent", ref.id());
        assertEquals("1.0.0", ref.version());
    }

    @Test
    void shouldThrowForUnknownAgent() {
        AgentForge forge = AgentForge.bootstrap();
        assertThrows(ResolutionException.class, () -> forge.agent("unknown"));
    }

    // --- Agent Execution ---

    @Test
    void shouldExecuteAgent() {
        DefaultAgentForge forge = (DefaultAgentForge) AgentForge.bootstrap();
        forge.registerAgent("echo-agent", new FunctionalAgentRef("echo-agent", "1.0.0",
            input -> ExecutionResult.success("exec-1", Map.of("echo", input.get("msg")))));

        ExecutionResult result = forge.agent("echo-agent").run(Map.of("msg", "hello"));
        assertTrue(result.isSuccess());
        assertEquals("hello", result.output().get("echo"));
    }

    @Test
    void shouldExecuteAgentAsync() throws Exception {
        DefaultAgentForge forge = (DefaultAgentForge) AgentForge.bootstrap();
        forge.registerAgent("async-agent", new FunctionalAgentRef("async-agent", "1.0.0",
            input -> ExecutionResult.success("exec-2", Map.of("done", true))));

        CompletionStage<ExecutionResult> stage = forge.agent("async-agent").runAsync(Map.of());
        ExecutionResult result = stage.toCompletableFuture().get();
        assertTrue(result.isSuccess());
    }

    // --- Capability ---

    @Test
    void shouldInvokeCapability() {
        DefaultAgentForge forge = (DefaultAgentForge) AgentForge.bootstrap();
        forge.registerCapability("calc.add", new FunctionalCapabilityRef("calc.add", "1.0.0",
            input -> InvocationResult.success(Map.of("sum", 3))));

        InvocationResult result = forge.capability("calc.add").invoke(Map.of("a", 1, "b", 2));
        assertTrue(result.success());
        assertEquals(3, result.output().get("sum"));
    }

    @Test
    void shouldThrowForUnknownCapability() {
        AgentForge forge = AgentForge.bootstrap();
        assertThrows(ResolutionException.class, () -> forge.capability("unknown"));
    }

    // --- Graph ---

    @Test
    void shouldExecuteGraph() {
        DefaultAgentForge forge = (DefaultAgentForge) AgentForge.bootstrap();
        forge.registerGraph("test-graph", new FunctionalGraphRef("test-graph", "1.0.0",
            input -> ExecutionResult.success("exec-3", Map.of("result", "ok"))));

        ExecutionResult result = forge.graph("test-graph").run(Map.of());
        assertTrue(result.isSuccess());
        assertEquals("ok", result.output().get("result"));
    }

    // --- ExecutionOptions ---

    @Test
    void shouldBuildExecutionOptions() {
        ExecutionOptions options = ExecutionOptions.builder()
            .timeout(Duration.ofSeconds(30))
            .streaming(true)
            .idempotencyKey("key-1")
            .metadata(Map.of("trace", "abc"))
            .build();
        assertEquals(Duration.ofSeconds(30), options.timeout());
        assertTrue(options.isStreaming());
        assertEquals("key-1", options.idempotencyKey());
    }

    // --- StateView ---

    @Test
    void shouldCreateStateViewFromMap() {
        StateView view = StateView.ofMap(Map.of("key", "value", "count", 42));
        assertEquals("value", view.<String>get("key"));
        assertEquals(42, view.<Integer>get("count"));
        assertTrue(view.contains("key"));
        assertFalse(view.contains("missing"));
        assertEquals(2, view.snapshot().size());
    }

    // --- Listing ---

    @Test
    void shouldListRegisteredIds() {
        DefaultAgentForge forge = (DefaultAgentForge) AgentForge.bootstrap();
        forge.registerAgent("a1", new FunctionalAgentRef("a1", "1.0.0", i -> ExecutionResult.success("e", Map.of())));
        forge.registerAgent("a2", new FunctionalAgentRef("a2", "1.0.0", i -> ExecutionResult.success("e", Map.of())));
        forge.registerCapability("c1", new FunctionalCapabilityRef("c1", "1.0.0", i -> InvocationResult.success(Map.of())));
        forge.registerGraph("g1", new FunctionalGraphRef("g1", "1.0.0", i -> ExecutionResult.success("e", Map.of())));

        assertEquals(2, forge.agentIds().size());
        assertEquals(1, forge.capabilityIds().size());
        assertEquals(1, forge.graphIds().size());
    }

    // --- Exception hierarchy ---

    @Test
    void shouldCreateSpecificExceptions() {
        assertInstanceOf(AgentForgeException.class, new ConfigurationException("bad config"));
        assertInstanceOf(AgentForgeException.class, new ResolutionException("Agent", "x"));
        assertInstanceOf(AgentForgeException.class, new AuthorizationException("denied"));
        assertInstanceOf(AgentForgeException.class, new PolicyDeniedException("policy"));
        assertInstanceOf(AgentForgeException.class, new BudgetExceededException("budget"));
        assertInstanceOf(AgentForgeException.class, new CapabilityInvocationException("cap", "failed"));
    }
}
