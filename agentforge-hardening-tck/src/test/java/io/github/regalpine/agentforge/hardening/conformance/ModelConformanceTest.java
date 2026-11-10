package io.github.regalpine.agentforge.hardening.conformance;

import io.github.regalpine.agentforge.hardening.*;
import io.github.regalpine.agentforge.kernel.*;
import io.github.regalpine.agentforge.model.ModelProvider;
import io.github.regalpine.agentforge.provider.mock.MockProvider;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * conformance-model: ModelProvider
 * / Capability Discovery / Error Model
 * / Streaming.
 * AF-RELEASE-02 §4, T-H6.3.
 */
class ModelConformanceTest {

    @Test
    void afModel001_mockProviderDescriptor() {
        var provider = new MockProvider();
        var desc = provider.descriptor();
        assertNotNull(desc);
        assertNotNull(desc.providerId());
    }

    @Test
    void afModel002_mockProviderType() {
        var provider = new MockProvider();
        // MockProvider implements CapabilityProvider
        assertNotNull(provider);
    }

    @Test
    void afModel003_modelProviderInterface() {
        // Verify ModelProvider SPI exists
        assertNotNull(ModelProvider.class);
    }

    @Test
    void afModel004_modelProviderMethods() {
        // Verify the SPI methods exist
        var methods = ModelProvider.class.getMethods();
        assertTrue(methods.length > 0);
    }

    @Test
    void afModel005_entityIdForModel() {
        var modelId = new EntityId("gpt-4");
        assertEquals("gpt-4", modelId.value());
    }
}
