package io.github.regalpine.agentforge.kernel.config;

/**
 * Root configuration for AgentForge Server.
 *
 * <p>Immutable record carrying all server configuration.
 * Loaded by {@link ConfigLoader} with priority:
 * environment variables → agentforge.properties → defaults.
 *
 * @param server      HTTP server settings
 * @param security    IAM and authentication settings
 * @param database    persistence settings (reserved for future use)
 * @param llm         LLM provider defaults
 * @param observability metrics and tracing settings
 */
public record AgentForgeConfig(
    ServerConfig server,
    SecurityConfig security,
    DatabaseConfig database,
    RedisConfig redis,
    LlmConfig llm,
    ObservabilityConfig observability
) {

    /**
     * Create a config with all defaults.
     */
    public static AgentForgeConfig defaults() {
        return new AgentForgeConfig(
            ServerConfig.defaults(),
            SecurityConfig.defaults(),
            DatabaseConfig.defaults(),
            RedisConfig.defaults(),
            LlmConfig.defaults(),
            ObservabilityConfig.defaults()
        );
    }

    /**
     * HTTP server configuration.
     *
     * @param port         listen port (default 8080)
     * @param workerThreads Netty/event-loop thread count (0 = 2 × CPU cores)
     * @param corsOrigins  allowed CORS origins (comma-separated, default "*")
     */
    public record ServerConfig(
        int port,
        int workerThreads,
        String corsOrigins
    ) {
        public static ServerConfig defaults() {
            return new ServerConfig(8080, 0, "*");
        }
    }

    /**
     * Security / IAM configuration.
     *
     * @param adminPassword     initial admin password (required for first boot)
     * @param skipPasswordCheck bypass password check for local dev
     * @param defaultTenantId   tenant ID for single-tenant mode
     * @param sessionTimeoutMinutes session token TTL
     * @param sessionSigningKeyHex hex-encoded shared HMAC key (≥32 bytes); required in production
     */
    public record SecurityConfig(
        String adminPassword,
        boolean skipPasswordCheck,
        String defaultTenantId,
        int sessionTimeoutMinutes,
        String sessionSigningKeyHex
    ) {
        public static SecurityConfig defaults() {
            return new SecurityConfig(null, false, "default", 60, null);
        }
    }

    /**
     * Database configuration (reserved for future persistence backends).
     *
     * @param type    "memory" | "postgresql"
     * @param url     JDBC URL (for postgresql)
     * @param username DB username
     * @param password DB password
     */
    public record DatabaseConfig(
        String type,
        String url,
        String username,
        String password
    ) {
        public static DatabaseConfig defaults() {
            return new DatabaseConfig("memory", null, null, null);
        }
    }

    /**
     * Redis configuration for distributed coordination.
     *
     * @param uri Redis connection URI (e.g. "redis://localhost:6379")
     */
    public record RedisConfig(
        String uri
    ) {
        public static RedisConfig defaults() {
            return new RedisConfig(null);
        }
    }

    /**
     * LLM provider defaults.
     *
     * @param defaultProvider default provider ID (e.g. "openai")
     * @param defaultModel    default model ID (e.g. "gpt-4")
     * @param apiKey          API key for the default provider
     * @param baseUrl         custom base URL for the provider (optional)
     */
    public record LlmConfig(
        String defaultProvider,
        String defaultModel,
        String apiKey,
        String baseUrl
    ) {
        public static LlmConfig defaults() {
            return new LlmConfig("openai", "gpt-4", null, null);
        }
    }

    /**
     * Observability configuration.
     *
     * @param enabled        whether metrics collection is enabled
     * @param otlpEndpoint   OTLP exporter endpoint (empty = disabled)
     * @param metricsPrefix  metric name prefix
     */
    public record ObservabilityConfig(
        boolean enabled,
        String otlpEndpoint,
        String metricsPrefix
    ) {
        public static ObservabilityConfig defaults() {
            return new ObservabilityConfig(true, "", "agentforge.");
        }
    }
}
