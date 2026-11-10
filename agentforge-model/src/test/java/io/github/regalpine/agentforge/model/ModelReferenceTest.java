package io.github.regalpine.agentforge.model;

import io.github.regalpine.agentforge.kernel.EntityId;
import io.github.regalpine.agentforge.kernel.Version;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ModelReferenceTest {

    @Test
    void shouldCreateModelReference() {
        ModelReference ref = ModelReference.of("openai", "gpt-4", "1.0.0");

        assertEquals("openai", ref.providerId().value());
        assertEquals("gpt-4", ref.modelId().value());
        assertEquals("1.0.0", ref.version().value());
    }

    @Test
    void shouldCreateModelReferenceWithDefaultVersion() {
        ModelReference ref = ModelReference.of("openai", "gpt-4");

        assertEquals("openai", ref.providerId().value());
        assertEquals("gpt-4", ref.modelId().value());
        assertEquals("1.0.0", ref.version().value());
    }

    @Test
    void shouldRejectNullProvider() {
        assertThrows(NullPointerException.class, () ->
            new ModelReference(null, new EntityId("gpt-4"), new Version("1.0.0"))
        );
    }

    @Test
    void shouldRejectNullModel() {
        assertThrows(NullPointerException.class, () ->
            new ModelReference(new EntityId("openai"), null, new Version("1.0.0"))
        );
    }

    @Test
    void shouldRejectNullVersion() {
        assertThrows(NullPointerException.class, () ->
            new ModelReference(new EntityId("openai"), new EntityId("gpt-4"), null)
        );
    }

    @Test
    void shouldHaveCorrectToString() {
        ModelReference ref = ModelReference.of("openai", "gpt-4", "1.0.0");
        assertEquals("openai:gpt-4:1.0.0", ref.toString());
    }

    @Test
    void shouldBeEqualForSameValues() {
        ModelReference ref1 = ModelReference.of("openai", "gpt-4", "1.0.0");
        ModelReference ref2 = ModelReference.of("openai", "gpt-4", "1.0.0");

        assertEquals(ref1, ref2);
        assertEquals(ref1.hashCode(), ref2.hashCode());
    }
}
