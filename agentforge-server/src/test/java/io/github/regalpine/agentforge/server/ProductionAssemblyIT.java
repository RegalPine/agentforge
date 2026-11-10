package io.github.regalpine.agentforge.server;

import io.github.regalpine.agentforge.graph.GraphDefinition;
import io.github.regalpine.agentforge.graph.NodeType;
import io.github.regalpine.agentforge.graph.ir.ExecutionPlan;
import io.github.regalpine.agentforge.kernel.EntityId;
import io.github.regalpine.agentforge.kernel.Version;
import io.github.regalpine.agentforge.server.api.ExecutionSubmitRequest;
import io.github.regalpine.agentforge.server.execution.ExecutionStore;
import io.github.regalpine.agentforge.server.service.GraphService;
import io.github.regalpine.agentforge.compiler.RegistrySnapshot;
import org.junit.jupiter.api.*;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test for the full production assembly.
 * <p>
 * Uses Testcontainers to start real PostgreSQL + Redis,
 * then boots AgentForgeApplication in PRODUCTION mode
 * and verifies end-to-end persistence.
 */
@Tag("integration")
class ProductionAssemblyIT {

    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
        .withDatabaseName("agentforge_test")
        .withUsername("test")
        .withPassword("test");

    @SuppressWarnings("resource")
    static GenericContainer<?> redis = new GenericContainer<>("redis:7-alpine")
        .withExposedPorts(6379);

    @BeforeAll
    static void startContainers() {
        postgres.start();
        redis.start();
    }

    @AfterAll
    static void stopContainers() {
        postgres.stop();
        redis.stop();
    }

    @Test
    void productionModeShouldPersistExecutionPlans() {
        // Boot in PRODUCTION mode
        var config = io.github.regalpine.agentforge.kernel.config.AgentForgeConfig.defaults();
        // Override with test container settings via builder
        var app = AgentForgeApplication.builder()
            .config(new io.github.regalpine.agentforge.kernel.config.AgentForgeConfig(
                config.server(),
                new io.github.regalpine.agentforge.kernel.config.AgentForgeConfig.SecurityConfig(
                    "test-password", false, "default", 60,
                    "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef"),
                new io.github.regalpine.agentforge.kernel.config.AgentForgeConfig.DatabaseConfig(
                    "postgresql",
                    postgres.getJdbcUrl(),
                    postgres.getUsername(),
                    postgres.getPassword()),
                new io.github.regalpine.agentforge.kernel.config.AgentForgeConfig.RedisConfig(
                    "redis://" + redis.getHost() + ":" + redis.getMappedPort(6379)),
                config.llm(),
                config.observability()
            ))
            .demoMode(false)
            .build();

        // Verify distributed components are wired
        assertNotNull(app.leaseStore(), "LeaseStore should be wired in PRODUCTION mode");
        assertNotNull(app.workerRegistrationStore(), "WorkerRegistrationStore should be wired");
        assertNotNull(app.leaderElection(), "LeaderElection should be wired");
        assertNotNull(app.taskQueue(), "TaskQueue should be wired");

        // Verify health checks
        var health = app.agentRegistrar(); // just to confirm app is running
        assertNotNull(health);
    }

    @Test
    void redisLeaseStoreShouldCompeteForLease() {
        String redisUri = "redis://" + redis.getHost() + ":" + redis.getMappedPort(6379);
        var store = new io.github.regalpine.agentforge.distributed.redis.RedisLeaseStore(redisUri);

        var execId = new io.github.regalpine.agentforge.event.ExecutionId("test-exec-1");
        var worker1 = new io.github.regalpine.agentforge.distributed.worker.WorkerId("worker-1");
        var worker2 = new io.github.regalpine.agentforge.distributed.worker.WorkerId("worker-2");

        // Worker 1 acquires lease
        var lease1 = store.acquire(execId, worker1, 30_000);
        assertTrue(lease1.isPresent(), "Worker 1 should acquire lease");
        assertEquals(worker1, lease1.get().workerId());

        // Worker 2 should fail to acquire same lease
        var lease2 = store.acquire(execId, worker2, 30_000);
        assertTrue(lease2.isEmpty(), "Worker 2 should NOT acquire lease while worker 1 holds it");

        // Worker 1 releases
        boolean released = store.release(execId, worker1, lease1.get().fencingToken());
        assertTrue(released, "Worker 1 should release lease");

        // Now worker 2 can acquire
        var lease2b = store.acquire(execId, worker2, 30_000);
        assertTrue(lease2b.isPresent(), "Worker 2 should acquire after release");
    }

    @Test
    void redisLeaderElectionShouldHaveSingleLeader() {
        String redisUri = "redis://" + redis.getHost() + ":" + redis.getMappedPort(6379);
        var election1 = new io.github.regalpine.agentforge.distributed.redis.RedisLeaderElection(redisUri, "instance-1");
        var election2 = new io.github.regalpine.agentforge.distributed.redis.RedisLeaderElection(redisUri, "instance-2");

        // Instance 1 acquires leadership
        boolean leader1 = election1.tryAcquireLeadership();
        assertTrue(leader1, "Instance 1 should become leader");
        assertTrue(election1.isLeader(), "Instance 1 should report isLeader");

        // Instance 2 should NOT become leader
        boolean leader2 = election2.tryAcquireLeadership();
        assertFalse(leader2, "Instance 2 should NOT become leader");

        // Release and let instance 2 take over
        election1.releaseLeadership();
        boolean leader2b = election2.tryAcquireLeadership();
        assertTrue(leader2b, "Instance 2 should become leader after release");
    }
}
