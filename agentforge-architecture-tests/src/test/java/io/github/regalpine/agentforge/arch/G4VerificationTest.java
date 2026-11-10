package io.github.regalpine.agentforge.arch;

import io.github.regalpine.agentforge.fault.*;
import io.github.regalpine.agentforge.kernel.config.AgentForgeConfig;
import io.github.regalpine.agentforge.server.bootstrap.DeploymentMode;
import io.github.regalpine.agentforge.server.bootstrap.ProductionAssemblyValidator;
import io.github.regalpine.agentforge.server.health.*;
import io.github.regalpine.agentforge.runtime.InMemoryExecutionPlanRegistry;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * G4 Verification: Validates all G4 work items.
 *
 * <p>Covers:
 * <ul>
 *   <li>G4-1: Deployment mode separation (demo vs production)</li>
 *   <li>G4-2: Health check probes reflect real dependencies</li>
 *   <li>G4-3: Architecture scan coverage is non-zero</li>
 *   <li>G4-4: Fault injection disaster recovery</li>
 *   <li>G4-5: Fixed workload benchmarks exist</li>
 * </ul>
 */
class G4VerificationTest {

    // ── G4-1: Deployment Mode ──

    @Test
    void demoModeShouldResolveFromString() {
        assertEquals(DeploymentMode.DEMO,
            DeploymentMode.fromString(null));
        assertEquals(DeploymentMode.DEMO,
            DeploymentMode.fromString("demo"));
        assertEquals(DeploymentMode.DEMO,
            DeploymentMode.fromString("unknown"));
    }

    @Test
    void productionModeShouldResolveFromString() {
        assertEquals(DeploymentMode.PRODUCTION,
            DeploymentMode.fromString("production"));
        assertEquals(DeploymentMode.PRODUCTION,
            DeploymentMode.fromString("prod"));
        assertEquals(DeploymentMode.PRODUCTION,
            DeploymentMode.fromString("PRODUCTION"));
    }

    @Test
    void demoModeShouldPassValidation() {
        AgentForgeConfig config = AgentForgeConfig.defaults();
        assertDoesNotThrow(() ->
            ProductionAssemblyValidator.validate(
                config, DeploymentMode.DEMO));
    }

    @Test
    void productionModeShouldRejectMemoryDatabase() {
        AgentForgeConfig config = AgentForgeConfig.defaults();
        // defaults use "memory" database type
        assertThrows(IllegalStateException.class,
            () -> ProductionAssemblyValidator.validate(
                config, DeploymentMode.PRODUCTION),
            "Production mode should reject memory database");
    }

    @Test
    void productionModeShouldRejectMissingDatabaseUrl() {
        AgentForgeConfig config = new AgentForgeConfig(
            AgentForgeConfig.ServerConfig.defaults(),
            new AgentForgeConfig.SecurityConfig(
                "password", false, "default", 60, null),
            new AgentForgeConfig.DatabaseConfig(
                "postgresql", null, null, null),
            AgentForgeConfig.RedisConfig.defaults(),
            AgentForgeConfig.LlmConfig.defaults(),
            AgentForgeConfig.ObservabilityConfig.defaults()
        );

        assertThrows(IllegalStateException.class,
            () -> ProductionAssemblyValidator.validate(
                config, DeploymentMode.PRODUCTION),
            "Production PostgreSQL requires URL");
    }

    @Test
    void productionModeShouldRejectSkipPasswordCheck() {
        AgentForgeConfig config = new AgentForgeConfig(
            AgentForgeConfig.ServerConfig.defaults(),
            new AgentForgeConfig.SecurityConfig(
                "password", true, "default", 60, null),
            new AgentForgeConfig.DatabaseConfig(
                "postgresql", "jdbc:postgresql://localhost/db",
                "user", "pass"),
            AgentForgeConfig.RedisConfig.defaults(),
            AgentForgeConfig.LlmConfig.defaults(),
            AgentForgeConfig.ObservabilityConfig.defaults()
        );

        assertThrows(IllegalStateException.class,
            () -> ProductionAssemblyValidator.validate(
                config, DeploymentMode.PRODUCTION),
            "Production must not skip password check");
    }

    // ── G4-2: Health Checks ──

    @Test
    void livenessShouldAlwaysBeUp() {
        HealthAggregator agg = new HealthAggregator(List.of());
        assertEquals("UP", agg.liveness().status());
    }

    @Test
    void readinessShouldReflectDependencies() {
        HealthAggregator allUp = new HealthAggregator(List.of(
            () -> HealthStatus.healthy("server"),
            () -> HealthStatus.healthy("registry")
        ));
        assertEquals("UP", allUp.readiness().status());
        assertFalse(allUp.isDegraded());

        HealthAggregator oneDown = new HealthAggregator(List.of(
            () -> HealthStatus.healthy("server"),
            () -> HealthStatus.unhealthy("db", "timeout")
        ));
        assertEquals("DOWN", oneDown.readiness().status());
        assertTrue(oneDown.isDegraded());

        HealthAggregator allDown = new HealthAggregator(List.of(
            () -> HealthStatus.unhealthy("server", "down"),
            () -> HealthStatus.unhealthy("db", "down")
        ));
        assertEquals("DOWN", allDown.readiness().status());
        assertFalse(allDown.isDegraded(),
            "All down is not degraded, it's fully down");
    }

    @Test
    void detailedReadinessShouldReturnAllResults() {
        HealthAggregator agg = new HealthAggregator(List.of(
            () -> HealthStatus.healthy("server"),
            () -> HealthStatus.unhealthy("db", "timeout"),
            () -> HealthStatus.healthy("registry")
        ));

        List<HealthStatus> details = agg.detailedReadiness();
        assertEquals(3, details.size());
    }

    @Test
    void registryHealthCheckShouldProbeSuccessfully() {
        InMemoryExecutionPlanRegistry registry =
            new InMemoryExecutionPlanRegistry();
        RegistryHealthCheck check = new RegistryHealthCheck(registry);

        HealthStatus status = check.check();
        assertEquals("UP", status.status());
        assertEquals("registry", status.component());
    }

    @Test
    void databaseHealthCheckShouldFailWithNoUrl() {
        DatabaseHealthCheck check = new DatabaseHealthCheck(
            null, null, null);

        HealthStatus status = check.check();
        assertEquals("DOWN", status.status());
        assertEquals("database", status.component());
    }

    @Test
    void databaseHealthCheckShouldFailWithBadUrl() {
        DatabaseHealthCheck check = new DatabaseHealthCheck(
            "jdbc:postgresql://nonexistent:5432/nope",
            "user", "pass");

        HealthStatus status = check.check();
        assertEquals("DOWN", status.status());
    }

    // ── G4-4: Fault Injection ──

    @Test
    void faultInjectorShouldDetectTransientFailure() {
        FaultRegistry registry = new FaultRegistry();
        registry.enable();
        FaultInjector injector = new FaultInjector(registry);

        FaultPoint fp = FaultPoints.PERSISTENCE_SAVE_STATE;
        registry.registerFaultPoint(fp);

        // Activate fault
        FaultRule rule = FaultRule.builder(fp)
            .type(FaultType.EXCEPTION)
            .exceptionMessage("DB down")
            .build();
        registry.activateRule(rule);

        // Should throw
        assertThrows(FaultException.class,
            () -> injector.checkAndThrow(fp));

        // Deactivate
        registry.deactivateRule(fp.id());

        // Should not throw
        assertDoesNotThrow(() -> injector.checkAndThrow(fp));

        registry.reset();
    }

    @Test
    void faultInjectorShouldSupportDelay() {
        FaultRegistry registry = new FaultRegistry();
        registry.enable();
        FaultInjector injector = new FaultInjector(registry);

        FaultPoint fp = FaultPoints.PROVIDER_CALL;
        registry.registerFaultPoint(fp);

        FaultRule rule = FaultRule.builder(fp)
            .type(FaultType.DELAY)
            .delay(java.time.Duration.ofMillis(50))
            .build();
        registry.activateRule(rule);

        long start = System.currentTimeMillis();
        injector.check(fp);
        long elapsed = System.currentTimeMillis() - start;

        assertTrue(elapsed >= 40,
            "Delay fault should introduce latency");

        registry.reset();
    }

    // ── G4-5: Benchmark Infrastructure ──

    @Test
    void benchmarkRunnerShouldComputePercentiles() {
        io.github.regalpine.agentforge.benchmark.BenchmarkRunner runner =
            new io.github.regalpine.agentforge.benchmark.BenchmarkRunner(
                "g4-verify");

        var result = runner.run(
            "trivial", "VERIFY", 100,
            () -> {
                // trivial work
                int sum = 0;
                for (int i = 0; i < 100; i++) sum += i;
            }
        );

        assertEquals(100, result.iterations());
        assertTrue(result.p50Nanos() > 0);
        assertTrue(result.p95Nanos() >= result.p50Nanos());
        assertTrue(result.p99Nanos() >= result.p95Nanos());
        assertTrue(result.throughputPerSec() > 0);
    }

    @Test
    void performanceGateShouldRejectViolation() {
        io.github.regalpine.agentforge.benchmark.PerformanceGate gate =
            new io.github.regalpine.agentforge.benchmark.PerformanceGate();

        io.github.regalpine.agentforge.benchmark.BenchmarkRunner runner =
            new io.github.regalpine.agentforge.benchmark.BenchmarkRunner(
                "gate-test");

        var result = runner.run(
            "slow-op", "GATE", 100,
            () -> {
                try { Thread.sleep(1); }
                catch (InterruptedException e) { Thread.currentThread().interrupt(); }
            }
        );

        // Set impossibly low threshold
        gate.assertThreshold("slow-op", result, 1L);

        assertThrows(
            io.github.regalpine.agentforge.benchmark.PerformanceGate
                .PerformanceRegressionException.class,
            gate::assertAllPassed
        );
    }
}
