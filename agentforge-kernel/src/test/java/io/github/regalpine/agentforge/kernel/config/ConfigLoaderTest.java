package io.github.regalpine.agentforge.kernel.config;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * BE-08: Tests for ConfigLoader priority resolution.
 */
class ConfigLoaderTest {

    @Test
    void loadWithDefaults() {
        Map<String, String> emptyEnv = new HashMap<>();
        AgentForgeConfig config = ConfigLoader.load(emptyEnv);

        assertEquals(8080, config.server().port());
        assertEquals(0, config.server().workerThreads());
        assertEquals("*", config.server().corsOrigins());
        assertNull(config.security().adminPassword());
        assertFalse(config.security().skipPasswordCheck());
        assertEquals("default", config.security().defaultTenantId());
        assertEquals("memory", config.database().type());
        assertEquals("openai", config.llm().defaultProvider());
        assertTrue(config.observability().enabled());
    }

    @Test
    void environmentVariablesOverrideDefaults() {
        Map<String, String> env = new HashMap<>();
        env.put("AGENTFORGE_SERVER_PORT", "9090");
        env.put("AGENTFORGE_ADMIN_PASSWORD", "secret123");
        env.put("AGENTFORGE_SKIP_PASSWORD_CHECK", "true");
        env.put("AGENTFORGE_LLM_PROVIDER", "anthropic");
        env.put("AGENTFORGE_LLM_MODEL", "claude-3");

        AgentForgeConfig config = ConfigLoader.load(env);

        assertEquals(9090, config.server().port());
        assertEquals("secret123", config.security().adminPassword());
        assertTrue(config.security().skipPasswordCheck());
        assertEquals("anthropic", config.llm().defaultProvider());
        assertEquals("claude-3", config.llm().defaultModel());
    }

    @Test
    void invalidIntegerFallsBackToDefault() {
        Map<String, String> env = new HashMap<>();
        env.put("AGENTFORGE_SERVER_PORT", "not-a-number");

        AgentForgeConfig config = ConfigLoader.load(env);

        assertEquals(8080, config.server().port());
    }

    @Test
    void booleanParsingAcceptsMultipleFormats() {
        Map<String, String> env = new HashMap<>();

        env.put("AGENTFORGE_SKIP_PASSWORD_CHECK", "yes");
        assertTrue(ConfigLoader.load(env).security().skipPasswordCheck());

        env.put("AGENTFORGE_SKIP_PASSWORD_CHECK", "1");
        assertTrue(ConfigLoader.load(env).security().skipPasswordCheck());

        env.put("AGENTFORGE_SKIP_PASSWORD_CHECK", "TRUE");
        assertTrue(ConfigLoader.load(env).security().skipPasswordCheck());

        env.put("AGENTFORGE_SKIP_PASSWORD_CHECK", "false");
        assertFalse(ConfigLoader.load(env).security().skipPasswordCheck());
    }

    @Test
    void databaseConfigFromEnvironment() {
        Map<String, String> env = new HashMap<>();
        env.put("AGENTFORGE_DATABASE_TYPE", "postgresql");
        env.put("AGENTFORGE_DATABASE_URL", "jdbc:postgresql://localhost/agentforge");
        env.put("AGENTFORGE_DATABASE_USERNAME", "af_user");
        env.put("AGENTFORGE_DATABASE_PASSWORD", "af_pass");

        AgentForgeConfig config = ConfigLoader.load(env);

        assertEquals("postgresql", config.database().type());
        assertEquals("jdbc:postgresql://localhost/agentforge", config.database().url());
        assertEquals("af_user", config.database().username());
        assertEquals("af_pass", config.database().password());
    }

    @Test
    void observabilityConfigFromEnvironment() {
        Map<String, String> env = new HashMap<>();
        env.put("AGENTFORGE_OTLP_ENDPOINT", "http://otel:4317");
        env.put("AGENTFORGE_OBSERVABILITY_ENABLED", "false");

        AgentForgeConfig config = ConfigLoader.load(env);

        assertFalse(config.observability().enabled());
        assertEquals("http://otel:4317", config.observability().otlpEndpoint());
    }

    @Test
    void defaultsFactoryMethod() {
        AgentForgeConfig config = AgentForgeConfig.defaults();

        assertNotNull(config.server());
        assertNotNull(config.security());
        assertNotNull(config.database());
        assertNotNull(config.llm());
        assertNotNull(config.observability());
        assertEquals(8080, config.server().port());
    }
}
