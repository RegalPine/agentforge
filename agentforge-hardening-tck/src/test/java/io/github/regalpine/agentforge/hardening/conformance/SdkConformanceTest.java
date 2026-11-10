package io.github.regalpine.agentforge.hardening.conformance;

import io.github.regalpine.agentforge.hardening.*;
import io.github.regalpine.agentforge.sdk.*;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * conformance-sdk: Fluent API / Builder
 * / Configuration.
 * AF-RELEASE-02 §4, T-H6.13.
 */
class SdkConformanceTest {

    @Test
    void afSdk001_errorCodeFormat() {
        var code = ErrorCode.model(1, "Error");
        assertEquals("AGF-MODEL-001", code.code());
    }

    @Test
    void afSdk002_errorCodeCatalog() {
        var catalog = ErrorCodeCatalog.all();
        assertFalse(catalog.isEmpty());
    }

    @Test
    void afSdk003_agentForgeBootstrap() {
        var forge = AgentForge.bootstrap();
        assertNotNull(forge);
        assertNotNull(forge.config());
    }

    @Test
    void afSdk004_memoryApi() {
        var forge = AgentForge.bootstrap();
        assertNotNull(forge.memory());
    }

    @Test
    void afSdk005_ragApi() {
        var forge = AgentForge.bootstrap();
        assertNotNull(forge.rag());
    }

    @Test
    void afSdk006_evaluationApi() {
        var forge = AgentForge.bootstrap();
        assertNotNull(forge.evaluation());
    }

    @Test
    void afSdk007_secretProviderEnv() {
        var sp = SecretProvider.fromEnvironment();
        var result = sp.resolve("NONEXISTENT_VAR_12345");
        assertTrue(result.isEmpty());
    }

    @Test
    void afSdk008_configDefaults() {
        var cfg = AgentForgeConfig.defaults();
        assertNotNull(cfg);
        assertNotNull(cfg.applicationId());
    }
}
