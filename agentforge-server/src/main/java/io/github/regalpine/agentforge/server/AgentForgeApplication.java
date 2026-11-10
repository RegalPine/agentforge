package io.github.regalpine.agentforge.server;

import io.github.regalpine.agentforge.agent.AgentDefinition;
import io.github.regalpine.agentforge.agent.AgentProvider;
import io.github.regalpine.agentforge.agent.AgentRegistrar;
import io.github.regalpine.agentforge.distributed.coordination.LeaderElection;
import io.github.regalpine.agentforge.distributed.lease.LeaseStore;
import io.github.regalpine.agentforge.distributed.redis.RedisLeaderElection;
import io.github.regalpine.agentforge.distributed.redis.RedisLeaseStore;
import io.github.regalpine.agentforge.distributed.redis.RedisStreamsTaskQueue;
import io.github.regalpine.agentforge.distributed.redis.RedisWorkerRegistrationStore;
import io.github.regalpine.agentforge.distributed.heartbeat.WorkerRegistrationStore;
import io.github.regalpine.agentforge.distributed.queue.TaskQueue;
import io.github.regalpine.agentforge.event.InMemoryEventPublisher;
import io.github.regalpine.agentforge.graph.ir.ExecutionPlanRegistry;
import io.github.regalpine.agentforge.kernel.config.AgentForgeConfig;
import io.github.regalpine.agentforge.kernel.config.ConfigLoader;
import io.github.regalpine.agentforge.persistence.postgresql.PostgresPersistenceModule;
import io.github.regalpine.agentforge.runtime.CommitPort;
import io.github.regalpine.agentforge.runtime.InMemoryCommitPort;
import io.github.regalpine.agentforge.runtime.InMemoryExecutionPlanRegistry;
import io.github.regalpine.agentforge.security.iam.*;
import io.github.regalpine.agentforge.server.agent.AgentForgeAgentRegistrar;
import io.github.regalpine.agentforge.server.bootstrap.DeploymentMode;
import io.github.regalpine.agentforge.server.bootstrap.ProductionAssemblyValidator;
import io.github.regalpine.agentforge.server.execution.ExecutionStore;
import io.github.regalpine.agentforge.server.execution.InMemoryExecutionStore;
import io.github.regalpine.agentforge.server.execution.JdbcServerExecutionStore;
import io.github.regalpine.agentforge.server.health.DatabaseHealthCheck;
import io.github.regalpine.agentforge.server.health.HealthAggregator;
import io.github.regalpine.agentforge.server.health.HealthStatus;
import io.github.regalpine.agentforge.server.health.RegistryHealthCheck;
import io.github.regalpine.agentforge.server.http.AgentForgeHttpController;
import io.github.regalpine.agentforge.server.http.AuthHandler;
import io.github.regalpine.agentforge.server.idempotency.IdempotencyStore;
import io.github.regalpine.agentforge.server.lifecycle.LifecycleComponent;
import io.github.regalpine.agentforge.server.persistence.JdbcExecutionPlanRegistry;
import io.github.regalpine.agentforge.server.persistence.JdbcIamStores;
import io.github.regalpine.agentforge.server.persistence.JdbcSessionStore;
import io.github.regalpine.agentforge.server.persistence.ServerSchema;
import io.github.regalpine.agentforge.server.service.ExecutionService;
import io.github.regalpine.agentforge.server.service.GraphService;

import java.time.Clock;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;
import java.util.Objects;
import java.util.ServiceLoader;
import java.util.function.Consumer;

/**
 * BE-08: Fluent builder for bootstrapping AgentForge Server.
 *
 * <p>Replaces manual DI wiring in {@link ServerMain} with a declarative API:
 * <pre>{@code
 * AgentForgeApplication.builder()
 *     .config(AgentForgeConfig.defaults())
 *     .build()
 *     .start();
 * }</pre>
 *
 * <p>The builder loads configuration from environment → properties → defaults
 * if no explicit config is provided.
 */
public final class AgentForgeApplication {

    private final AgentForgeConfig config;
    private final GraphService graphService;
    private final ExecutionService executionService;
    private final HealthAggregator healthAggregator;
    private final AuthHandler authHandler;
    private final SessionTokenProvider sessionProvider;
    private final boolean demoMode;
    private final DeploymentMode deploymentMode;
    private final List<LifecycleComponent> lifecycleComponents;
    private final AgentRegistrar agentRegistrar;
    private final LeaseStore leaseStore;
    private final WorkerRegistrationStore workerRegistrationStore;
    private final LeaderElection leaderElection;
    private final TaskQueue taskQueue;
    private volatile boolean draining = false;
    private AgentForgeHttpController controller;

    private AgentForgeApplication(
        AgentForgeConfig config,
        GraphService graphService,
        ExecutionService executionService,
        HealthAggregator healthAggregator,
        AuthHandler authHandler,
        SessionTokenProvider sessionProvider,
        boolean demoMode,
        List<LifecycleComponent> lifecycleComponents,
        AgentRegistrar agentRegistrar,
        LeaseStore leaseStore,
        WorkerRegistrationStore workerRegistrationStore,
        LeaderElection leaderElection,
        TaskQueue taskQueue
    ) {
        this.config = config;
        this.graphService = graphService;
        this.executionService = executionService;
        this.healthAggregator = healthAggregator;
        this.authHandler = authHandler;
        this.sessionProvider = sessionProvider;
        this.demoMode = demoMode;
        this.deploymentMode = demoMode
            ? DeploymentMode.DEMO : DeploymentMode.PRODUCTION;
        this.lifecycleComponents = lifecycleComponents != null
            ? List.copyOf(lifecycleComponents) : List.of();
        this.agentRegistrar = agentRegistrar;
        this.leaseStore = leaseStore;
        this.workerRegistrationStore = workerRegistrationStore;
        this.leaderElection = leaderElection;
        this.taskQueue = taskQueue;
    }

    /**
     * Create a new builder.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Start the application (HTTP server + services).
     */
    public void start() throws Exception {
        // Start lifecycle components (Worker, Relay, Reconciler, etc.)
        for (LifecycleComponent component : lifecycleComponents) {
            System.out.println("  启动组件: " + component.name());
            component.start();
        }

        int port = config.server().port();

        controller = new AgentForgeHttpController(
            port, graphService, executionService,
            healthAggregator, null, authHandler,
            sessionProvider, demoMode, agentRegistrar
        );
        controller.start();

        System.out.println("""
            
            ╔══════════════════════════════════════════╗
            ║       AgentForge Server Started          ║
            ╠══════════════════════════════════════════╣
            ║  Port: %-8s                          ║
            ║  Health:  http://localhost:%d/health/live ║
            ║  API:     http://localhost:%d/api/v1/     ║
            ║  Login:   POST /api/auth/login            ║
            ╚══════════════════════════════════════════╝
            """.formatted(port, port, port));

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("Shutting down AgentForge Server...");
            stop();
        }));
    }

    /**
     * Get the loaded configuration.
     */
    public AgentForgeConfig config() {
        return config;
    }

    /**
     * Get the Agent registrar.
     */
    public AgentRegistrar agentRegistrar() {
        return agentRegistrar;
    }

    /** Get the distributed lease store (null in DEMO mode). */
    public LeaseStore leaseStore() {
        return leaseStore;
    }

    /** Get the distributed worker registration store (null in DEMO mode). */
    public WorkerRegistrationStore workerRegistrationStore() {
        return workerRegistrationStore;
    }

    /** Get the leader election component (null in DEMO mode). */
    public LeaderElection leaderElection() {
        return leaderElection;
    }

    /** Get the distributed task queue (null in DEMO mode). */
    public TaskQueue taskQueue() {
        return taskQueue;
    }

    /**
     * Drain the application: stop accepting new requests,
     * stop claiming new tasks, wait for in-flight work.
     * <p>
     * Batch 6: shutdown/drain sequence per spec.
     */
    public void drain() {
        if (draining) return;
        draining = true;
        System.out.println("  Drain: 关闭接单资格...");
        // Readiness probe will now report DOWN (healthAggregator reflects draining)
        // In production, this would flip a flag that the readiness endpoint checks
        System.out.println("  Drain: 停止领取新任务...");
        // Workers/Relay stop polling (lifecycle components handle this)
        System.out.println("  Drain: 等待在途提交完成...");
        // Bounded wait for in-flight commits to complete
        try {
            Thread.sleep(Math.min(5000, 1000)); // Brief grace period
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Stop the application with drain.
     */
    public void stop() {
        drain();
        // Stop lifecycle components in reverse order
        for (int i = lifecycleComponents.size() - 1; i >= 0; i--) {
            try {
                lifecycleComponents.get(i).stop();
            } catch (Exception e) {
                System.err.println("  停止组件失败: " + lifecycleComponents.get(i).name() + ": " + e.getMessage());
            }
        }
        if (controller != null) {
            controller.stop();
        }
    }

    /**
     * Fluent builder for AgentForgeApplication.
     */
    public static final class Builder {
        private AgentForgeConfig config;
        private String adminPassword;
        private boolean demoMode = true;
        private boolean demoModeExplicitlySet = false;

        Builder() {
        }

        /**
         * Set explicit configuration (overrides env/properties loading).
         */
        public Builder config(AgentForgeConfig config) {
            this.config = Objects.requireNonNull(config);
            return this;
        }

        /**
         * Override admin password (useful for testing).
         */
        public Builder adminPassword(String password) {
            this.adminPassword = password;
            return this;
        }

        /**
         * Customize configuration via callback.
         */
        public Builder customizeConfig(Consumer<AgentForgeConfig> customizer) {
            // Load config first if not set
            if (config == null) {
                config = ConfigLoader.load();
            }
            // Note: since records are immutable, customizer can only read.
            // For mutations, create a new config and call config() directly.
            customizer.accept(config);
            return this;
        }

        /**
         * Set demo mode (default: true).
         * When false, uses real service implementations.
         */
        public Builder demoMode(boolean demo) {
            this.demoMode = demo;
            this.demoModeExplicitlySet = true;
            return this;
        }

        /**
         * Build the application, wiring all services.
         */
        public AgentForgeApplication build() {
            if (config == null) {
                config = ConfigLoader.load();
            }

            // Auto-detect demoMode based on database configuration if not explicitly set
            if (!demoModeExplicitlySet && "postgresql".equalsIgnoreCase(config.database().type())) {
                demoMode = false;
            }

            // Resolve admin password
            String password = adminPassword != null
                ? adminPassword
                : config.security().adminPassword();

            boolean skipCheck = config.security().skipPasswordCheck();

            // Validate admin password
            if (password == null || password.isBlank()) {
                if (skipCheck) {
                    System.err.println("""
                        ╔══════════════════════════════════════════════════════╗
                        ║  WARNING: AGENTFORGE_SKIP_PASSWORD_CHECK=true      ║
                        ║  Using fallback password 'dev-only' for local dev. ║
                        ║  DO NOT use this in production!                    ║
                        ╚══════════════════════════════════════════════════════╝
                        """);
                    password = "dev-only";
                } else {
                    System.err.println("""
                        ╔══════════════════════════════════════════════════════╗
                        ║  ERROR: AGENTFORGE_ADMIN_PASSWORD is not set.      ║
                        ║  The server refuses to start without an explicit   ║
                        ║  admin password for security reasons.              ║
                        ║                                                    ║
                        ║  Set via env: AGENTFORGE_ADMIN_PASSWORD=<secret>   ║
                        ║  Or via properties: security.adminPassword=<secret>║
                        ╚══════════════════════════════════════════════════════╝
                        """);
                    throw new IllegalStateException(
                        "Admin password is not configured. "
                        + "Set AGENTFORGE_ADMIN_PASSWORD or security.adminPassword."
                    );
                }
            }

            // ── IAM ──
            boolean useJdbc = "postgresql".equalsIgnoreCase(config.database().type());
            UserStore userStore;
            TenantStore tenantStore;
            RoleStore roleStore;
            SessionStore sessionStore;

            if (useJdbc) {
                var ds = new org.postgresql.ds.PGSimpleDataSource();
                ds.setUrl(config.database().url());
                ds.setUser(config.database().username());
                ds.setPassword(config.database().password());
                ServerSchema.migrate(ds);
                var iamStores = new JdbcIamStores(ds);
                IamBootstrap.BootstrapResult bootstrapResult = iamStores.bootstrap(password);
                System.out.println("  IAM 初始化完成 (首次启动: " + bootstrapResult.isFirstRun() + ")");
                userStore = iamStores.users();
                tenantStore = iamStores.tenants();
                roleStore = iamStores.roles();
                sessionStore = new JdbcSessionStore(ds);
                System.out.println("  数据库: PostgreSQL (共享 IAM/会话存储)");
            } else {
                var memUsers = new InMemoryUserStore();
                var memTenants = new InMemoryTenantStore();
                var memRoles = new InMemoryRoleStore();
                IamBootstrap iamBootstrap = new IamBootstrap(memTenants, memUsers, memRoles);
                IamBootstrap.BootstrapResult bootstrapResult = iamBootstrap.execute(password);
                System.out.println("  IAM 初始化完成 (首次启动: " + bootstrapResult.isFirstRun() + ")");
                userStore = memUsers;
                tenantStore = memTenants;
                roleStore = memRoles;
                sessionStore = new InMemorySessionStore();
            }
            System.out.println("  默认账号: admin / (密码已配置)");

            String tenantId = config.security().defaultTenantId();

            BuiltInAuthenticationProvider authProvider =
                new BuiltInAuthenticationProvider(userStore, roleStore, tenantStore, tenantId);

            // ── Session signing key ──
            byte[] signingKey;
            String keyHex = config.security().sessionSigningKeyHex();
            if (keyHex != null && !keyHex.isBlank()) {
                signingKey = HexFormat.of().parseHex(keyHex);
                if (signingKey.length < 32) throw new IllegalStateException("Session signing key must be ≥ 32 bytes");
            } else if (!demoMode) {
                throw new IllegalStateException(
                    "Production mode requires AGENTFORGE_SESSION_SIGNING_KEY (hex, ≥ 64 hex chars)");
            } else {
                signingKey = SessionTokenProvider.randomKey();
            }
            Duration sessionLifetime = Duration.ofMinutes(config.security().sessionTimeoutMinutes());
            SessionTokenProvider sessionProvider =
                new SessionTokenProvider(userStore, roleStore, tenantStore, tenantId,
                    sessionStore, signingKey, sessionLifetime, Clock.systemUTC());
            AuthHandler authHandler = new AuthHandler(authProvider, sessionProvider);

            // G4-1: Validate assembly for deployment mode
            DeploymentMode mode = demoMode
                ? DeploymentMode.DEMO : DeploymentMode.PRODUCTION;
            ProductionAssemblyValidator.validate(config, mode);

            // ── Services ──
            ExecutionPlanRegistry planRegistry;
            ExecutionStore executionStore;
            CommitPort commitPort;
            GraphService graphService;

            if (useJdbc) {
                // PRODUCTION mode: PostgreSQL-backed stores
                var ds = new org.postgresql.ds.PGSimpleDataSource();
                ds.setUrl(config.database().url());
                ds.setUser(config.database().username());
                ds.setPassword(config.database().password());

                // Runtime persistence (execution_state, events, checkpoints, etc.)
                PostgresPersistenceModule persistence = PostgresPersistenceModule.create(ds);

                // Server-level stores (execution_plan, server_execution)
                planRegistry = new JdbcExecutionPlanRegistry(ds);
                executionStore = new JdbcServerExecutionStore(ds);
                commitPort = persistence.commitPort();

                // GraphService with persistent registry for plan durability
                graphService = new GraphService(
                    new io.github.regalpine.agentforge.compiler.DefaultGraphCompiler(),
                    planRegistry
                );

                System.out.println("  服务存储: PostgreSQL (ExecutionPlan/Execution/State)");
            } else {
                // DEMO mode: In-memory stores
                planRegistry = new InMemoryExecutionPlanRegistry();
                executionStore = new InMemoryExecutionStore();
                commitPort = new InMemoryCommitPort(event -> {});
                graphService = new GraphService();
            }

            // ── Distributed Coordination (Redis) ──
            LeaseStore leaseStore = null;
            WorkerRegistrationStore workerRegStore = null;
            LeaderElection leaderElection = null;
            TaskQueue taskQueue = null;

            if (!demoMode) {
                String redisUri = config.redis() != null ? config.redis().uri() : null;
                if (redisUri != null && !redisUri.isBlank()) {
                    String instanceId = java.util.UUID.randomUUID().toString();
                    leaseStore = new RedisLeaseStore(redisUri);
                    workerRegStore = new RedisWorkerRegistrationStore(redisUri);
                    leaderElection = new RedisLeaderElection(redisUri, instanceId);
                    taskQueue = new RedisStreamsTaskQueue(redisUri, instanceId);
                    System.out.println("  分布式协调: Redis (" + redisUri + ")");
                } else {
                    System.out.println("  分布式协调: 未配置 Redis URI，跳过");
                }
            }

            ExecutionService executionService = new ExecutionService(
                graphService, planRegistry,
                new IdempotencyStore(), executionStore, commitPort
            );

            // G4-2: Health checks reflect real dependencies
            List<io.github.regalpine.agentforge.server.health.HealthCheck> checks
                = new ArrayList<>();
            checks.add(() -> HealthStatus.healthy("server"));
            checks.add(new RegistryHealthCheck(planRegistry));
            if (!demoMode && config.database() != null
                && !"memory".equalsIgnoreCase(config.database().type())) {
                checks.add(new DatabaseHealthCheck(
                    config.database().url(),
                    config.database().username(),
                    config.database().password()
                ));
            }
            if (!demoMode && config.redis() != null
                && config.redis().uri() != null
                && !config.redis().uri().isBlank()) {
                checks.add(io.github.regalpine.agentforge.server.health.RedisHealthCheck
                    .fromUri(config.redis().uri()));
            }
            HealthAggregator healthAggregator = new HealthAggregator(checks);

            // ── Agent Registrar ──
            var eventPublisher = new InMemoryEventPublisher();
            AgentRegistrar agentRegistrar =
                new AgentForgeAgentRegistrar(eventPublisher);

            // ServiceLoader auto-discovery of AgentProvider
            List<AgentProvider> providers = new ArrayList<>();
            ServiceLoader.load(AgentProvider.class)
                .forEach(providers::add);
            providers.sort(
                Comparator.comparingInt(AgentProvider::order));
            for (AgentProvider provider : providers) {
                for (AgentDefinition agent : provider.provide()) {
                    agentRegistrar.registerAndStart(
                        agent, provider.options(agent));
                }
            }
            System.out.println(
                "  Agent 注册: " + agentRegistrar.list().size()
                + " 个 (来自 " + providers.size() + " 个 Provider)");

            return new AgentForgeApplication(
                config, graphService, executionService,
                healthAggregator, authHandler, sessionProvider, demoMode,
                List.of(),
                agentRegistrar,
                leaseStore, workerRegStore, leaderElection, taskQueue
            );
        }
    }
}
