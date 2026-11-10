package io.github.regalpine.agentforge.sdk;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for H3 Developer Experience:
 * ErrorCode, Catalog, FluentBuilder,
 * YamlConfigLoader, SecretProvider,
 * MemoryApi, RagApi, EvaluationApi.
 */
class H3DeveloperExperienceTest {

    // --- ErrorCode ---

    @Test
    void errorCodeShouldFormatCorrectly() {
        ErrorCode code = ErrorCode.model(1,
            "Model provider not available");

        assertEquals("AGF-MODEL-001", code.code());
        assertEquals("MODEL", code.domain());
        assertEquals(1, code.number());
    }

    @Test
    void errorCodeShouldPadNumbers() {
        ErrorCode code = ErrorCode.core(42,
            "Test error");

        assertEquals("AGF-CORE-042", code.code());
    }

    @Test
    void errorCodeDomainsShouldWork() {
        assertEquals("AGF-CAP-001",
            ErrorCode.cap(1, "x").code());
        assertEquals("AGF-GRAPH-002",
            ErrorCode.graph(2, "x").code());
        assertEquals("AGF-EXEC-003",
            ErrorCode.exec(3, "x").code());
        assertEquals("AGF-RUNTIME-004",
            ErrorCode.runtime(4, "x").code());
        assertEquals("AGF-MEM-005",
            ErrorCode.mem(5, "x").code());
        assertEquals("AGF-RAG-006",
            ErrorCode.rag(6, "x").code());
        assertEquals("AGF-MCP-007",
            ErrorCode.mcp(7, "x").code());
        assertEquals("AGF-SEC-008",
            ErrorCode.sec(8, "x").code());
        assertEquals("AGF-GOV-009",
            ErrorCode.gov(9, "x").code());
        assertEquals("AGF-EVAL-010",
            ErrorCode.evaluation(10, "x")
                .code());
        assertEquals("AGF-CLI-011",
            ErrorCode.cli(11, "x").code());
    }

    // --- ErrorCodeCatalog ---

    @Test
    void catalogShouldContainAllDomains() {
        assertTrue(ErrorCodeCatalog.size() > 30);
        assertNotNull(
            ErrorCodeCatalog.lookup("AGF-CORE-001")
        );
        assertNotNull(
            ErrorCodeCatalog.lookup("AGF-MODEL-001")
        );
        assertNotNull(
            ErrorCodeCatalog.lookup("AGF-CLI-001")
        );
    }

    @Test
    void catalogShouldFilterByDomain() {
        var coreCodes =
            ErrorCodeCatalog.forDomain("CORE");

        assertFalse(coreCodes.isEmpty());
        assertTrue(
            coreCodes.containsKey("AGF-CORE-001")
        );
    }

    @Test
    void catalogShouldReturnNullForUnknown() {
        assertNull(
            ErrorCodeCatalog.lookup("AGF-XYZ-999")
        );
    }

    // --- FluentBuilder ---

    @Test
    void fluentBuilderShouldCreateForge() {
        try (AgentForge forge = FluentBuilder.create()
            .applicationId("test-app")
            .tenantId("t1")
            .build()
        ) {
            assertEquals("test-app",
                forge.config().applicationId());
            assertEquals("t1",
                forge.config().tenantId());
        }
    }

    @Test
    void fluentBuilderShouldCreateAgent() {
        AgentBuilder builder =
            FluentBuilder.create()
                .agent("my-agent");

        AgentDescriptor desc = builder
            .model("openai", "gpt-4")
            .capability("search")
            .description("Test agent")
            .build();

        assertEquals("my-agent", desc.id());
        assertEquals("Test agent", desc.description());
        assertTrue(desc.capabilities()
            .contains("search"));
    }

    @Test
    void fluentBuilderShouldCreateGraph() {
        GraphBuilder builder =
            FluentBuilder.create()
                .graph("my-graph");

        GraphDescriptor desc = builder
            .node("n1", "cap1")
            .node("n2", "cap2")
            .edge("n1", "n2")
            .description("Test graph")
            .build();

        assertEquals("my-graph", desc.graphId());
        assertEquals(2, desc.nodes().size());
        assertEquals(1, desc.edges().size());
    }

    // --- YamlConfigLoader ---

    @Test
    void yamlParserShouldParseFlat() {
        String yaml = """
            application:
              id: my-app
              tenant: t1
            model:
              provider: openai
            """;

        Map<String, Object> parsed =
            YamlConfigLoader.parseSimpleYaml(yaml);

        assertEquals("my-app",
            parsed.get("application.id"));
        assertEquals("t1",
            parsed.get("application.tenant"));
        assertEquals("openai",
            parsed.get("model.provider"));
    }

    @Test
    void yamlParserShouldHandleComments() {
        String yaml = """
            # This is a comment
            key: value
            # Another comment
            """;

        Map<String, Object> parsed =
            YamlConfigLoader.parseSimpleYaml(yaml);

        assertEquals("value", parsed.get("key"));
        assertEquals(1, parsed.size());
    }

    @Test
    void yamlParserShouldParseBooleans() {
        String yaml = """
            enabled: true
            disabled: false
            """;

        Map<String, Object> parsed =
            YamlConfigLoader.parseSimpleYaml(yaml);

        assertEquals(Boolean.TRUE,
            parsed.get("enabled"));
        assertEquals(Boolean.FALSE,
            parsed.get("disabled"));
    }

    @Test
    void yamlLoaderShouldReturnDefaults() {
        AgentForgeConfig config =
            YamlConfigLoader.loadFrom(
                java.nio.file.Path.of("/nonexistent")
            );

        assertNotNull(config);
        assertEquals("default",
            config.applicationId());
    }

    // --- SecretProvider ---

    @Test
    void mapSecretProviderShouldResolve() {
        SecretProvider sp =
            SecretProvider.fromMap(Map.of(
                "API_KEY", "secret-123"
            ));

        assertTrue(sp.canResolve("API_KEY"));
        assertEquals("secret-123",
            sp.resolve("API_KEY").orElse(null));
        assertFalse(sp.canResolve("MISSING"));
    }

    @Test
    void chainedProviderShouldFallback() {
        SecretProvider first =
            SecretProvider.fromMap(Map.of(
                "KEY_A", "val-a"
            ));
        SecretProvider second =
            SecretProvider.fromMap(Map.of(
                "KEY_B", "val-b"
            ));

        SecretProvider chain =
            SecretProvider.chain(first, second);

        assertEquals("val-a",
            chain.resolve("KEY_A").orElse(null));
        assertEquals("val-b",
            chain.resolve("KEY_B").orElse(null));
        assertTrue(chain.resolve("KEY_C").isEmpty());
    }

    // --- MemoryApi ---

    @Test
    void memoryApiShouldStoreAndRetrieve() {
        MemoryApi mem = new MemoryApi("USER");

        mem.save("m1", "content-1");
        mem.save("m2", "content-2");

        assertEquals(2, mem.size());
        assertTrue(mem.exists("m1"));
        assertEquals("content-1",
            mem.get("m1").orElse(null));

        mem.delete("m1");
        assertEquals(1, mem.size());
        assertFalse(mem.exists("m1"));
    }

    @Test
    void memoryApiShouldReportScope() {
        MemoryApi mem = new MemoryApi("AGENT");
        assertEquals("AGENT", mem.defaultScope());
    }

    // --- RagApi ---

    @Test
    void ragApiShouldUpsertAndSearch() {
        RagApi rag = new RagApi("pgvector");

        rag.upsert("c1", "chunk-1");
        rag.upsert("c2", "chunk-2");

        assertEquals(2, rag.size());
        assertEquals("pgvector", rag.indexType());

        var results = rag.search("query");
        assertEquals(2, results.size());

        rag.delete("c1");
        assertEquals(1, rag.size());
    }

    // --- EvaluationApi ---

    @Test
    void evaluationApiShouldManageCases() {
        EvaluationApi eval = new EvaluationApi();

        eval.registerCase("case-1", "eval-obj");
        assertEquals(1, eval.caseCount());
        assertTrue(eval.getCase("case-1").isPresent());

        eval.recordResult("run-1", "result-obj");
        assertEquals(1, eval.resultCount());
        assertTrue(
            eval.getResult("run-1").isPresent()
        );
    }

    // --- AgentForge integration ---

    @Test
    void agentForgeShouldExposeAllApis() {
        try (AgentForge forge = AgentForge.bootstrap()) {
            assertNotNull(forge.memory());
            assertNotNull(forge.rag());
            assertNotNull(forge.evaluation());
            assertNotNull(
                AgentForge.builder()
            );
        }
    }
}
