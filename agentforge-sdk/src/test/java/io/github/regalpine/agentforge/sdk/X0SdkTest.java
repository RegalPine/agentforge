package io.github.regalpine.agentforge.sdk;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * X0 SDK tests.
 * AF-ENHANCE-01 §13-17: Fluent API,
 * Agent interface, Bootstrap.
 */
class X0SdkTest {

    @Test
    void agentForgeAgentBuilderCreatesAgent() {
        Agent agent = AgentForge.defineAgent("test-agent")
            .model("openai", "gpt-4")
            .skill("research")
            .tool("web-search")
            .policy("safety")
            .workspace("exec-ws")
            .systemPrompt("You are helpful.")
            .description("A test agent")
            .buildAgent();

        assertNotNull(agent);
        assertEquals("test-agent", agent.id());
        assertEquals("test-agent",
            agent.metadata().name());
    }

    @Test
    void agentExecuteReturnsResult() {
        Agent agent = AgentForge.defineAgent("exec-agent")
            .model("mock", "test-model")
            .buildAgent();

        ExecutionResult result =
            agent.execute(
                AgentInput.of("Hello")
            );

        assertNotNull(result);
        assertTrue(result.isSuccess());
        assertEquals("exec-agent",
            result.output().get("agentId"));
    }

    @Test
    void agentRunShorthand() {
        Agent agent = AgentForge.defineAgent("run-agent")
            .buildAgent();

        ExecutionResult result =
            agent.run("Test input");

        assertNotNull(result);
        assertTrue(result.isSuccess());
    }

    @Test
    void agentForgeRunSimplePath() {
        ExecutionResult result =
            AgentForge.run(
                "simple-agent",
                "Analyze trends"
            );

        assertNotNull(result);
        assertTrue(result.isSuccess());
    }

    @Test
    void agentForgeRunWithAgent() {
        Agent agent = AgentForge.defineAgent("built-agent")
            .model("mock", "test")
            .buildAgent();

        ExecutionResult result =
            AgentForge.run(agent, "Test");

        assertNotNull(result);
        assertTrue(result.isSuccess());
    }

    @Test
    void agentBuilderWithSkillRef() {
        SkillRef skill =
            SkillRef.of("research", "2.0.0");

        Agent agent = AgentForge.defineAgent("skill-agent")
            .skill(skill)
            .buildAgent();

        assertNotNull(agent);
    }

    @Test
    void agentBuilderWithPolicyRef() {
        PolicyRef policy =
            PolicyRef.of("safety-policy");

        Agent agent = AgentForge.defineAgent("policy-agent")
            .policy(policy)
            .buildAgent();

        assertNotNull(agent);
    }

    @Test
    void agentRuntimeBuilder() {
        Agent agent = AgentForge.defineAgent("rt-agent")
            .buildAgent();

        try (AgentRuntime runtime =
            AgentForge.runtime()
                .agent(agent)
                .build()
        ) {
            assertNotNull(runtime);
            assertEquals(1,
                runtime.agentIds().size());
            assertTrue(
                runtime.agentIds()
                    .contains("rt-agent")
            );
        }
    }

    @Test
    void agentRuntimeRun() {
        Agent agent = AgentForge.defineAgent("run-rt")
            .buildAgent();

        try (AgentRuntime runtime =
            AgentForge.runtime()
                .agent(agent)
                .build()
        ) {
            ExecutionResult result =
                runtime.run("run-rt", "Hello");
            assertTrue(result.isSuccess());
        }
    }

    @Test
    void agentInputBuilder() {
        AgentInput input = AgentInput.builder()
            .message("Test message")
            .parameter("key", "value")
            .conversationId("conv-1")
            .sessionId("sess-1")
            .build();

        assertEquals("Test message",
            input.message());
        assertEquals("value",
            input.parameters().get("key"));
        assertEquals("conv-1",
            input.conversationId());
        assertEquals("sess-1",
            input.sessionId());
    }

    @Test
    void agentMetadataBuilder() {
        AgentMetadata metadata =
            AgentMetadata.builder("agent-1")
                .name("Agent One")
                .version("2.0.0")
                .description("Test agent")
                .label("env", "test")
                .build();

        assertEquals("agent-1", metadata.id());
        assertEquals("Agent One",
            metadata.name());
        assertEquals("2.0.0",
            metadata.version());
        assertEquals("test",
            metadata.labels().get("env"));
    }

    @Test
    void skillRefQualifiedId() {
        SkillRef ref = new SkillRef(
            "ns", "search", "1.0.0"
        );
        assertEquals("ns:search:1.0.0",
            ref.qualifiedId());
    }

    @Test
    void agentStreamEmitsEvents() {
        Agent agent = AgentForge.defineAgent("stream-agent")
            .buildAgent();

        java.util.List<AgentEvent> events =
            new java.util.ArrayList<>();

        ExecutionResult result = agent.stream(
            AgentInput.of("Hello"),
            events::add
        );

        assertNotNull(result);
        assertTrue(result.isSuccess());
        assertFalse(events.isEmpty());
        assertEquals("execution.started",
            events.get(0).type());
    }
}
