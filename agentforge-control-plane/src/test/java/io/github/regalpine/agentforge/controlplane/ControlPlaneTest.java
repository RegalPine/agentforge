package io.github.regalpine.agentforge.controlplane;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Control Plane tests.
 * AF-ENHANCE-14 §4-12.
 */
class ControlPlaneTest {

    // ---- DeploymentId ----

    @Test
    void deploymentIdCreation() {
        var id = new DeploymentId(
            "prod", "research-agent");
        assertEquals("prod",
            id.namespace());
        assertEquals("research-agent",
            id.name());
        assertEquals(
            "prod/research-agent",
            id.qualifiedName());
    }

    @Test
    void deploymentIdValidation() {
        assertThrows(
            IllegalArgumentException.class,
            () -> new DeploymentId(
                null, "name"));
        assertThrows(
            IllegalArgumentException.class,
            () -> new DeploymentId(
                "ns", null));
        assertThrows(
            IllegalArgumentException.class,
            () -> new DeploymentId(
                "", "name"));
    }

    // ---- DeploymentSpec ----

    @Test
    void deploymentSpecCreation() {
        var spec = DeploymentSpec.of(
            "1.4.0");
        assertEquals("1.4.0",
            spec.agentVersion());
        assertEquals(1,
            spec.minReplicas());
        assertEquals(1,
            spec.maxReplicas());
        assertEquals(
            DeploymentStrategy.ROLLING,
            spec.strategy());
    }

    @Test
    void deploymentSpecFull() {
        var spec = new DeploymentSpec(
            "1.4.0", "sha256:abc",
            2, 10,
            DeploymentStrategy.CANARY,
            "prod-policy",
            Map.of("cpu", "2"),
            Map.of()
        );
        assertEquals(2,
            spec.minReplicas());
        assertEquals(10,
            spec.maxReplicas());
        assertEquals(
            DeploymentStrategy.CANARY,
            spec.strategy());
        assertEquals("prod-policy",
            spec.policyReference());
    }

    @Test
    void deploymentSpecValidation() {
        assertThrows(
            IllegalArgumentException.class,
            () -> DeploymentSpec.of(null));
    }

    // ---- DeploymentStatus ----

    @Test
    void deploymentStatusChecks() {
        assertTrue(
            DeploymentStatus.ACTIVE
                .isAvailable());
        assertTrue(
            DeploymentStatus.SCALING
                .isAvailable());
        assertFalse(
            DeploymentStatus.CREATED
                .isAvailable());
        assertTrue(
            DeploymentStatus.STOPPED
                .isTerminal());
        assertTrue(
            DeploymentStatus.DEGRADED
                .isDegraded());
    }

    // ---- InMemoryDeployment ----

    @Test
    void deploymentLifecycle() {
        var id = new DeploymentId(
            "prod", "agent-a");
        var env = EnvironmentId
            .PRODUCTION;
        var spec = DeploymentSpec.of(
            "1.0.0");

        var dep = InMemoryDeployment.of(
            id, "agent-a", env, spec);
        assertEquals(
            DeploymentStatus.CREATED,
            dep.status());

        dep = dep.activate();
        assertEquals(
            DeploymentStatus.ACTIVE,
            dep.status());
        assertTrue(
            dep.status().isAvailable());

        dep = dep.stop();
        assertEquals(
            DeploymentStatus.STOPPED,
            dep.status());
        assertTrue(
            dep.status().isTerminal());
    }

    @Test
    void deploymentRevision() {
        var id = new DeploymentId(
            "prod", "agent-a");
        var dep = InMemoryDeployment.of(
            id, "agent-a",
            EnvironmentId.PRODUCTION,
            DeploymentSpec.of("1.0.0"));

        var rev = DeploymentRevision.of(
            "rev-001", id, "1.0.0");
        dep = dep.addRevision(rev);

        assertEquals(1,
            dep.revisions().size());
        assertEquals("rev-001",
            dep.revisions().get(0)
                .revisionId());
    }

    // ---- DeploymentRevision ----

    @Test
    void revisionImmutability() {
        var id = new DeploymentId(
            "ns", "name");
        var rev = DeploymentRevision.of(
            "rev-001", id, "1.0.0");
        assertNotNull(rev.createdAt());
        assertNotNull(
            rev.agentVersion());
    }

    @Test
    void revisionValidation() {
        assertThrows(
            IllegalArgumentException.class,
            () -> DeploymentRevision.of(
                null, null, null));
    }

    // ---- EnvironmentId ----

    @Test
    void environmentConstants() {
        assertEquals("dev",
            EnvironmentId.DEV.value());
        assertEquals("test",
            EnvironmentId.TEST.value());
        assertEquals("staging",
            EnvironmentId.STAGING
                .value());
        assertEquals("production",
            EnvironmentId.PRODUCTION
                .value());
    }

    @Test
    void customEnvironment() {
        var env = new EnvironmentId(
            "sandbox");
        assertEquals("sandbox",
            env.value());
    }

    // ---- Environment ----

    @Test
    void environmentCreation() {
        var env = Environment.of(
            EnvironmentId.DEV);
        assertEquals("dev",
            env.envId().value());
        assertEquals("dev",
            env.type());
        assertTrue(
            env.config().isEmpty());
    }

    // ---- InstanceStatus ----

    @Test
    void instanceStatusChecks() {
        assertTrue(
            InstanceStatus.READY
                .isAvailable());
        assertTrue(
            InstanceStatus.RUNNING
                .isAvailable());
        assertFalse(
            InstanceStatus.STARTING
                .isAvailable());
        assertTrue(
            InstanceStatus.STOPPED
                .isTerminal());
        assertTrue(
            InstanceStatus.FAILED
                .isTerminal());
        assertTrue(
            InstanceStatus.DRAINING
                .isDraining());
    }

    // ---- InMemoryRuntimeInstance ----

    @Test
    void instanceLifecycle() {
        var depId = new DeploymentId(
            "ns", "name");
        var inst =
            InMemoryRuntimeInstance.of(
                "inst-1", depId,
                "rev-001");
        assertEquals(
            InstanceStatus.CREATED,
            inst.status());
        assertEquals("inst-1",
            inst.id());
        assertEquals(depId,
            inst.deployment());

        inst = inst.start();
        assertEquals(
            InstanceStatus.RUNNING,
            inst.status());
        assertTrue(
            inst.health().isHealthy());

        inst = inst.stop();
        assertEquals(
            InstanceStatus.STOPPED,
            inst.status());
    }

    // ---- RuntimePool ----

    @Test
    void runtimePoolCreation() {
        var depId = new DeploymentId(
            "ns", "name");
        var pool = RuntimePool.of(
            "pool-1", depId);
        assertEquals("pool-1",
            pool.poolId());
        assertEquals(0,
            pool.availableCount());
        assertFalse(pool.isHealthy());
    }

    @Test
    void runtimePoolWithInstances() {
        var depId = new DeploymentId(
            "ns", "name");
        var inst1 =
            InMemoryRuntimeInstance.of(
                "i1", depId, "r1")
                .start();
        var inst2 =
            InMemoryRuntimeInstance.of(
                "i2", depId, "r1")
                .start();

        var pool = new RuntimePool(
            "pool-1", depId,
            List.of(inst1, inst2),
            2, 5, Map.of());
        assertEquals(2,
            pool.availableCount());
        assertTrue(pool.isHealthy());
    }

    // ---- RuntimeHealth ----

    @Test
    void healthChecks() {
        var healthy = new RuntimeHealth(
            HealthStatus.HEALTHY,
            HealthStatus.HEALTHY,
            10, Instant.now());
        assertTrue(
            healthy.isHealthy());

        var unhealthy = new RuntimeHealth(
            HealthStatus.HEALTHY,
            HealthStatus.UNHEALTHY,
            0, Instant.now());
        assertFalse(
            unhealthy.isHealthy());

        var unknown =
            RuntimeHealth.unknown();
        assertFalse(
            unknown.isHealthy());
    }

    // ---- Configuration ----

    @Test
    void configurationCreation() {
        var config = Configuration.of(
            "model-config", "1.0",
            Map.of("temp", "0.7"));
        assertEquals("model-config",
            config.configId());
        assertEquals("1.0",
            config.version());
        assertEquals("0.7",
            config.entries()
                .get("temp"));
    }

    @Test
    void configurationValidation() {
        assertThrows(
            IllegalArgumentException.class,
            () -> Configuration.of(
                null, null, null));
    }

    // ---- SecretReference ----

    @Test
    void secretReferenceCreation() {
        var ref = SecretReference.of(
            "vault", "api-key");
        assertEquals("vault",
            ref.provider());
        assertEquals("api-key",
            ref.key());
    }

    @Test
    void secretReferenceValidation() {
        assertThrows(
            IllegalArgumentException.class,
            () -> SecretReference.of(
                null, null));
    }

    // ---- ResourceQuota ----

    @Test
    void quotaCreation() {
        var quota = ResourceQuota.of(
            "tenant-quota",
            QuotaScope.TENANT);
        assertEquals("tenant-quota",
            quota.quotaId());
        assertEquals(
            QuotaScope.TENANT,
            quota.scope());
    }

    @Test
    void quotaLimitCheck() {
        var quota = new ResourceQuota(
            "q1", QuotaScope.DEPLOYMENT,
            Map.of("cpu", 10L),
            Map.of("cpu", 5L));
        assertTrue(
            quota.isWithinLimit("cpu"));

        var over = new ResourceQuota(
            "q2", QuotaScope.DEPLOYMENT,
            Map.of("cpu", 10L),
            Map.of("cpu", 15L));
        assertFalse(
            over.isWithinLimit("cpu"));

        assertTrue(
            quota.isWithinLimit("gpu"));
    }

    // ---- RuntimePolicy ----

    @Test
    void policyDefaults() {
        var policy =
            RuntimePolicy.defaults(
                "prod-policy");
        assertEquals("prod-policy",
            policy.policyId());
        assertEquals(10,
            policy
                .maxConcurrentExecutions());
        assertEquals(3,
            policy.maxRetries());
    }

    // ---- Rollout ----

    @Test
    void rolloutLifecycle() {
        var depId = new DeploymentId(
            "ns", "name");
        var rollout = Rollout.of(
            "ro-1", depId, "rev-001",
            DeploymentStrategy.ROLLING);
        assertEquals(
            RolloutStatus.CREATED,
            rollout.status());
        assertEquals(0,
            rollout.progress());

        rollout = rollout.advance(50);
        assertEquals(
            RolloutStatus.IN_PROGRESS,
            rollout.status());
        assertEquals(50,
            rollout.progress());

        rollout = rollout.advance(100);
        assertEquals(
            RolloutStatus.COMPLETED,
            rollout.status());
    }

    @Test
    void rolloutRollback() {
        var depId = new DeploymentId(
            "ns", "name");
        var rollout = Rollout.of(
            "ro-1", depId, "rev-001",
            DeploymentStrategy.CANARY);
        rollout = rollout.rollback();
        assertEquals(
            RolloutStatus.ROLLED_BACK,
            rollout.status());
    }

    // ---- Release ----

    @Test
    void releaseLifecycle() {
        var depId = new DeploymentId(
            "ns", "name");
        var release = Release.of(
            "rel-1", depId, "rev-001",
            EnvironmentId.PRODUCTION);
        assertEquals(
            ReleaseStatus.CREATED,
            release.status());

        release = release.approve();
        assertEquals(
            ReleaseStatus.APPROVED,
            release.status());

        release = release.deploy();
        assertEquals(
            ReleaseStatus.ACTIVE,
            release.status());
        assertTrue(
            release.status().isLive());
    }

    @Test
    void releaseRollback() {
        var depId = new DeploymentId(
            "ns", "name");
        var release = Release.of(
            "rel-1", depId, "rev-001",
            EnvironmentId.PRODUCTION);
        release = release.deploy();
        release = release.rollback();
        assertEquals(
            ReleaseStatus.ROLLED_BACK,
            release.status());
        assertTrue(
            release.status()
                .isTerminal());
    }

    // ---- ControlPlaneEvent ----

    @Test
    void eventCreation() {
        var depId = new DeploymentId(
            "ns", "name");
        var event = ControlPlaneEvent.of(
            "evt-1",
            "deployment.created",
            depId);
        assertEquals("evt-1",
            event.eventId());
        assertEquals(
            "deployment.created",
            event.type());
        assertNotNull(
            event.timestamp());
    }

    // ---- Strategy / Isolation enums ----

    @Test
    void strategyValues() {
        assertEquals(4,
            DeploymentStrategy.values()
                .length);
    }

    @Test
    void isolationValues() {
        assertEquals(4,
            RuntimeIsolation.values()
                .length);
    }

    @Test
    void loadBalancingValues() {
        assertEquals(4,
            LoadBalancingStrategy
                .values().length);
    }

    @Test
    void secretStatusValues() {
        assertEquals(4,
            SecretStatus.values().length);
    }

    @Test
    void releaseStatusChecks() {
        assertTrue(
            ReleaseStatus.ACTIVE
                .isLive());
        assertTrue(
            ReleaseStatus.DEPLOYING
                .isLive());
        assertTrue(
            ReleaseStatus.FAILED
                .isTerminal());
        assertTrue(
            ReleaseStatus.REJECTED
                .isTerminal());
        assertTrue(
            ReleaseStatus.ROLLED_BACK
                .isTerminal());
    }
}
