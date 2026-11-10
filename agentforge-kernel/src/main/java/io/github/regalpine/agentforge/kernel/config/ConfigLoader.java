package io.github.regalpine.agentforge.kernel.config;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.Properties;

/**
 * Loads {@link AgentForgeConfig} from multiple sources with priority:
 * <ol>
 *   <li>Environment variables (AGENTFORGE_*)</li>
 *   <li>agentforge.properties on classpath</li>
 *   <li>Built-in defaults</li>
 * </ol>
 *
 * <p>Environment variable naming convention:
 * {@code AGENTFORGE_<SECTION>_<KEY>} (e.g. AGENTFORGE_SERVER_PORT).
 */
public final class ConfigLoader {

    private static final String ENV_PREFIX = "AGENTFORGE_";
    private static final String PROPERTIES_FILE = "agentforge.properties";

    private ConfigLoader() {
    }

    /**
     * Load configuration from environment → properties → defaults.
     */
    public static AgentForgeConfig load() {
        return load(System.getenv());
    }

    /**
     * Load configuration with explicit environment map (for testing).
     */
    public static AgentForgeConfig load(Map<String, String> env) {
        Properties fileProps = loadPropertiesFile();
        AgentForgeConfig defaults = AgentForgeConfig.defaults();

        // Server
        // Backward compat: AGENTFORGE_PORT maps to server.port
        String portEnv = env.getOrDefault(ENV_PREFIX + "SERVER_PORT",
            env.getOrDefault(ENV_PREFIX + "PORT", null));
        AgentForgeConfig.ServerConfig server = new AgentForgeConfig.ServerConfig(
            portEnv != null ? parseIntOr(portEnv, defaults.server().port()) : defaults.server().port(),
            resolveInt(env, fileProps, "WORKER_THREADS", "server.workerThreads", defaults.server().workerThreads()),
            resolveString(env, fileProps, "CORS_ORIGINS", "server.corsOrigins", defaults.server().corsOrigins())
        );

        // Security
        AgentForgeConfig.SecurityConfig security = new AgentForgeConfig.SecurityConfig(
            resolveString(env, fileProps, "ADMIN_PASSWORD", "security.adminPassword", defaults.security().adminPassword()),
            resolveBoolean(env, fileProps, "SKIP_PASSWORD_CHECK", "security.skipPasswordCheck", defaults.security().skipPasswordCheck()),
            resolveString(env, fileProps, "DEFAULT_TENANT_ID", "security.defaultTenantId", defaults.security().defaultTenantId()),
            resolveInt(env, fileProps, "SESSION_TIMEOUT_MINUTES", "security.sessionTimeoutMinutes", defaults.security().sessionTimeoutMinutes()),
            resolveString(env, fileProps, "SESSION_SIGNING_KEY", "security.sessionSigningKeyHex", defaults.security().sessionSigningKeyHex())
        );

        // Database
        AgentForgeConfig.DatabaseConfig database = new AgentForgeConfig.DatabaseConfig(
            resolveString(env, fileProps, "DATABASE_TYPE", "database.type", defaults.database().type()),
            resolveString(env, fileProps, "DATABASE_URL", "database.url", defaults.database().url()),
            resolveString(env, fileProps, "DATABASE_USERNAME", "database.username", defaults.database().username()),
            resolveString(env, fileProps, "DATABASE_PASSWORD", "database.password", defaults.database().password())
        );

        // Redis
        AgentForgeConfig.RedisConfig redis = new AgentForgeConfig.RedisConfig(
            resolveString(env, fileProps, "REDIS_URI", "redis.uri", defaults.redis().uri())
        );

        // LLM
        AgentForgeConfig.LlmConfig llm = new AgentForgeConfig.LlmConfig(
            resolveString(env, fileProps, "LLM_PROVIDER", "llm.defaultProvider", defaults.llm().defaultProvider()),
            resolveString(env, fileProps, "LLM_MODEL", "llm.defaultModel", defaults.llm().defaultModel()),
            resolveString(env, fileProps, "LLM_API_KEY", "llm.apiKey", defaults.llm().apiKey()),
            resolveString(env, fileProps, "LLM_BASE_URL", "llm.baseUrl", defaults.llm().baseUrl())
        );

        // Observability
        AgentForgeConfig.ObservabilityConfig observability = new AgentForgeConfig.ObservabilityConfig(
            resolveBoolean(env, fileProps, "OBSERVABILITY_ENABLED", "observability.enabled", defaults.observability().enabled()),
            resolveString(env, fileProps, "OTLP_ENDPOINT", "observability.otlpEndpoint", defaults.observability().otlpEndpoint()),
            resolveString(env, fileProps, "METRICS_PREFIX", "observability.metricsPrefix", defaults.observability().metricsPrefix())
        );

        return new AgentForgeConfig(server, security, database, redis, llm, observability);
    }

    private static Properties loadPropertiesFile() {
        Properties props = new Properties();
        try (InputStream is = ConfigLoader.class.getClassLoader()
                .getResourceAsStream(PROPERTIES_FILE)) {
            if (is != null) {
                props.load(is);
            }
        } catch (IOException e) {
            System.err.println("WARNING: Failed to load " + PROPERTIES_FILE + ": " + e.getMessage());
        }
        return props;
    }

    private static String resolveString(
        Map<String, String> env, Properties fileProps,
        String envKey, String propKey, String defaultValue
    ) {
        // 1. Environment variable
        String envVal = env.get(ENV_PREFIX + envKey);
        if (envVal != null && !envVal.isBlank()) {
            return envVal;
        }
        // 2. Properties file
        String propVal = fileProps.getProperty(propKey);
        if (propVal != null && !propVal.isBlank()) {
            return propVal;
        }
        // 3. Default
        return defaultValue;
    }

    private static int resolveInt(
        Map<String, String> env, Properties fileProps,
        String envKey, String propKey, int defaultValue
    ) {
        String str = resolveString(env, fileProps, envKey, propKey, null);
        if (str != null) {
            try {
                return Integer.parseInt(str);
            } catch (NumberFormatException e) {
                System.err.println("WARNING: Invalid integer for " + envKey + ": " + str);
            }
        }
        return defaultValue;
    }

    private static boolean resolveBoolean(
        Map<String, String> env, Properties fileProps,
        String envKey, String propKey, boolean defaultValue
    ) {
        String str = resolveString(env, fileProps, envKey, propKey, null);
        if (str != null) {
            return "true".equalsIgnoreCase(str) || "1".equals(str) || "yes".equalsIgnoreCase(str);
        }
        return defaultValue;
    }

    private static int parseIntOr(String value, int fallback) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return fallback;
        }
    }
}
