package io.github.regalpine.agentforge.runtime.structured;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class OutputProcessingPipelineTest {

    @Test
    void shouldParseSimpleJson() {
        OutputProcessingPipeline pipeline = OutputProcessingPipeline.defaults();
        StructuredResult result = pipeline.process("{\"name\": \"Alice\", \"age\": 30}");

        assertTrue(result.success());
        assertEquals("Alice", result.data().get("name"));
        assertEquals(30, result.data().get("age"));
    }

    @Test
    void shouldParseJsonFromMarkdownBlock() {
        OutputProcessingPipeline pipeline = OutputProcessingPipeline.defaults();
        String raw = "Here is the result:\n```json\n{\"key\": \"value\"}\n```\nDone.";
        StructuredResult result = pipeline.process(raw);

        assertTrue(result.success());
        assertEquals("value", result.data().get("key"));
    }

    @Test
    void shouldParseEmbeddedJson() {
        OutputProcessingPipeline pipeline = OutputProcessingPipeline.defaults();
        String raw = "The answer is {\"result\": 42} as expected.";
        StructuredResult result = pipeline.process(raw);

        assertTrue(result.success());
        assertEquals(42, result.data().get("result"));
    }

    @Test
    void shouldFailOnInvalidJson() {
        OutputProcessingPipeline pipeline = OutputProcessingPipeline.defaults();
        StructuredResult result = pipeline.process("not json at all");

        assertFalse(result.success());
        assertTrue(result.report().hasErrors());
    }

    @Test
    void shouldFailOnBlankInput() {
        OutputProcessingPipeline pipeline = OutputProcessingPipeline.defaults();
        StructuredResult result = pipeline.process("   ");

        assertFalse(result.success());
    }

    @Test
    void shouldNormalizeNulls() {
        OutputProcessingPipeline pipeline = OutputProcessingPipeline.builder()
            .addStage(new JsonParserStage())
            .addStage(new NormalizerStage(true, true))
            .build();

        // The parser doesn't produce nulls from valid JSON, but let's test the stage directly
        PipelineContext ctx = new PipelineContext();
        NormalizerStage stage = new NormalizerStage(true, true);
        Map<String, Object> input = new java.util.LinkedHashMap<>();
        input.put("key", null);
        input.put("name", "  Alice  ");
        Map<String, Object> result = stage.process(input, ctx);

        assertFalse(result.containsKey("key"));
        assertEquals("Alice", result.get("name"));
    }

    @Test
    void shouldTrimStrings() {
        OutputProcessingPipeline pipeline = OutputProcessingPipeline.defaults();
        StructuredResult result = pipeline.process("{\"name\": \"  Alice  \"}");

        assertTrue(result.success());
        assertEquals("Alice", result.data().get("name"));
    }

    @Test
    void shouldHandleNestedObjects() {
        OutputProcessingPipeline pipeline = OutputProcessingPipeline.defaults();
        StructuredResult result = pipeline.process("{\"user\": {\"name\": \"Bob\", \"age\": 25}}");

        assertTrue(result.success());
        assertTrue(result.data().get("user") instanceof Map);
        @SuppressWarnings("unchecked")
        Map<String, Object> user = (Map<String, Object>) result.data().get("user");
        assertEquals("Bob", user.get("name"));
    }

    @Test
    void shouldHandleBooleanAndNull() {
        OutputProcessingPipeline pipeline = OutputProcessingPipeline.defaults();
        StructuredResult result = pipeline.process("{\"active\": true, \"deleted\": false}");

        assertTrue(result.success());
        assertEquals(true, result.data().get("active"));
        assertEquals(false, result.data().get("deleted"));
    }

    @Test
    void shouldHandleArrays() {
        OutputProcessingPipeline pipeline = OutputProcessingPipeline.defaults();
        StructuredResult result = pipeline.process("{\"items\": [1, 2, 3]}");

        assertTrue(result.success());
        assertTrue(result.data().get("items") instanceof java.util.List);
    }

    @Test
    void shouldReportProcessingTime() {
        OutputProcessingPipeline pipeline = OutputProcessingPipeline.defaults();
        StructuredResult result = pipeline.process("{\"key\": \"value\"}");

        assertTrue(result.report().processingTimeMillis() >= 0);
    }
}
