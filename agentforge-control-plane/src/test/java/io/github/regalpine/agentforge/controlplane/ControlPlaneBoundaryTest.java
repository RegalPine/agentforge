package io.github.regalpine.agentforge.controlplane;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Deep boundary / immutability /
 * edge-case tests for control-plane.
 * Complements ControlPlaneTest.
 */
class ControlPlaneBoundaryTest {

    private static final DeploymentId DEP_ID =
        new DeploymentId("ns", "agent");

    // ===== InMemoryDeployment =====

    @Test
    void deployment_rejectsNullId() {
        assertThrows(
            IllegalArgumentException.class,
            () -> InMemoryDeployment.of(
                null, "agent",
                EnvironmentId.DEV,
                DeploymentSpec.of("1.0.0")
            )
        );
    }

    @Test
    void deployment_rejectsBlankAgentRef() {
        assertThrows(
            IllegalArgumentException.class,
            () -> InMemoryDeployment.of(
                DEP_ID, " ",
                EnvironmentId.DEV,
                DeploymentSpec.of("1.0.0")
            )
        );
    }

    @Test
    void deployment_rejectsNullAgentRef() {
        assertThrows(
            IllegalArgumentException.class,
            () -> InMemoryDeployment.of(
                DEP_ID, null,
                EnvironmentId.DEV,
                DeploymentSpec.of("1.0.0")
            )
        );
    }

    @Test
    void deployment_rejectsNullEnv() {
        assertThrows(
            IllegalArgumentException.class,
            () -> InMemoryDeployment.of(
                DEP_ID, "agent", null,
                DeploymentSpec.of("1.0.0")
            )
        );
    }

    @Test
    void deployment_rejectsNullSpec() {
        assertThrows(
            IllegalArgumentException.class,
            () -> InMemoryDeployment.of(
                DEP_ID, "agent",
                EnvironmentId.DEV, null
            )
        );
    }

    @Test
    void deployment_unmodifiableRevisions() {
        var dep = InMemoryDeployment.of(
            DEP_ID, "agent",
            EnvironmentId.DEV,
            DeploymentSpec.of("1.0.0")
        );

        assertThrows(
            UnsupportedOperationException.class,
            () -> dep.revisions().add(null)
        );
    }

    @Test
    void deployment_unmodifiableMetadata() {
        var dep = InMemoryDeployment.of(
            DEP_ID, "agent",
            EnvironmentId.DEV,
            DeploymentSpec.of("1.0.0")
        );

        assertThrows(
            UnsupportedOperationException.class,
            () -> dep.metadata().put("k", "v")
        );
    }

    @Test
    void deployment_defensiveCopyRevisions() {
        var rev = DeploymentRevision.of(
            "r1", DEP_ID, "1.0.0"
        );
        var list = new ArrayList<>(
            List.of(rev)
        );

        var dep = new InMemoryDeployment(
            DEP_ID, "agent",
            EnvironmentId.DEV,
            DeploymentSpec.of("1.0.0"),
            null, list, null, null
        );

        list.add(
            DeploymentRevision.of(
                "r2", DEP_ID, "2.0.0"
            )
        );
        assertEquals(1, dep.revisions().size());
    }

    @Test
    void deployment_addMultipleRevisions() {
        var dep = InMemoryDeployment.of(
            DEP_ID, "agent",
            EnvironmentId.DEV,
            DeploymentSpec.of("1.0.0")
        );

        dep = dep.addRevision(
            DeploymentRevision.of(
                "r1", DEP_ID, "1.0.0"
            )
        );
        dep = dep.addRevision(
            DeploymentRevision.of(
                "r2", DEP_ID, "2.0.0"
            )
        );

        assertEquals(2, dep.revisions().size());
    }

    @Test
    void deployment_activatePreservesFields() {
        var dep = InMemoryDeployment.of(
            DEP_ID, "agent",
            EnvironmentId.PRODUCTION,
            DeploymentSpec.of("1.0.0")
        );

        var active = dep.activate();
        assertEquals(DEP_ID, active.id());
        assertEquals("agent", active.agentReference());
        assertEquals(
            EnvironmentId.PRODUCTION,
            active.environment()
        );
    }

    // ===== InMemoryRuntimeInstance =====

    @Test
    void instance_rejectsNullId() {
        assertThrows(
            IllegalArgumentException.class,
            () -> InMemoryRuntimeInstance.of(
                null, DEP_ID, "r1"
            )
        );
    }

    @Test
    void instance_rejectsBlankId() {
        assertThrows(
            IllegalArgumentException.class,
            () -> InMemoryRuntimeInstance.of(
                " ", DEP_ID, "r1"
            )
        );
    }

    @Test
    void instance_rejectsNullDeployment() {
        assertThrows(
            IllegalArgumentException.class,
            () -> InMemoryRuntimeInstance.of(
                "i1", null, "r1"
            )
        );
    }

    @Test
    void instance_startIsHealthy() {
        var inst = InMemoryRuntimeInstance.of(
            "i1", DEP_ID, "r1"
        ).start();

        assertTrue(inst.health().isHealthy());
        assertEquals(10, inst.health().capacity());
    }

    @Test
    void instance_stopPreservesId() {
        var inst = InMemoryRuntimeInstance.of(
            "i1", DEP_ID, "r1"
        ).start().stop();

        assertEquals("i1", inst.id());
        assertEquals(DEP_ID, inst.deployment());
        assertEquals("r1", inst.revisionId());
    }

    @Test
    void instance_unmodifiableMetadata() {
        var inst = InMemoryRuntimeInstance.of(
            "i1", DEP_ID, "r1"
        );

        assertThrows(
            UnsupportedOperationException.class,
            () -> inst.metadata().put("k", "v")
        );
    }

    // ===== Release =====

    @Test
    void release_rejectsBlankId() {
        assertThrows(
            IllegalArgumentException.class,
            () -> Release.of(
                " ", DEP_ID, "r1",
                EnvironmentId.DEV
            )
        );
    }

    @Test
    void release_rejectsNullDeployment() {
        assertThrows(
            IllegalArgumentException.class,
            () -> Release.of(
                "rel-1", null, "r1",
                EnvironmentId.DEV
            )
        );
    }

    @Test
    void release_rejectsNullEnv() {
        assertThrows(
            IllegalArgumentException.class,
            () -> Release.of(
                "rel-1", DEP_ID, "r1", null
            )
        );
    }

    @Test
    void release_defensiveCopyMetadata() {
        Map<String, String> meta = new HashMap<>();
        meta.put("a", "b");

        var release = new Release(
            "rel-1", DEP_ID, "r1",
            EnvironmentId.DEV, null,
            null, null, meta
        );

        meta.put("c", "d");
        assertEquals(1, release.metadata().size());
    }

    @Test
    void release_unmodifiableMetadata() {
        var release = Release.of(
            "rel-1", DEP_ID, "r1",
            EnvironmentId.DEV
        );

        assertThrows(
            UnsupportedOperationException.class,
            () -> release.metadata().put("k", "v")
        );
    }

    @Test
    void release_approvePreservesFields() {
        var release = Release.of(
            "rel-1", DEP_ID, "r1",
            EnvironmentId.PRODUCTION
        );

        var approved = release.approve();
        assertEquals("rel-1", approved.releaseId());
        assertEquals(DEP_ID, approved.deploymentId());
        assertEquals("r1", approved.revisionId());
    }

    // ===== Rollout =====

    @Test
    void rollout_rejectsBlankId() {
        assertThrows(
            IllegalArgumentException.class,
            () -> Rollout.of(
                " ", DEP_ID, "r1",
                DeploymentStrategy.ROLLING
            )
        );
    }

    @Test
    void rollout_rejectsNullDeployment() {
        assertThrows(
            IllegalArgumentException.class,
            () -> Rollout.of(
                "ro-1", null, "r1",
                DeploymentStrategy.ROLLING
            )
        );
    }

    @Test
    void rollout_advanceTo99IsInProgress() {
        var rollout = Rollout.of(
            "ro-1", DEP_ID, "r1",
            DeploymentStrategy.ROLLING
        );

        var advanced = rollout.advance(99);
        assertEquals(
            RolloutStatus.IN_PROGRESS,
            advanced.status()
        );
        assertEquals(99, advanced.progress());
    }

    @Test
    void rollout_advanceTo100IsCompleted() {
        var rollout = Rollout.of(
            "ro-1", DEP_ID, "r1",
            DeploymentStrategy.ROLLING
        );

        var completed = rollout.advance(100);
        assertEquals(
            RolloutStatus.COMPLETED,
            completed.status()
        );
    }

    @Test
    void rollout_advanceOver100IsCompleted() {
        var rollout = Rollout.of(
            "ro-1", DEP_ID, "r1",
            DeploymentStrategy.ROLLING
        );

        var completed = rollout.advance(150);
        assertEquals(
            RolloutStatus.COMPLETED,
            completed.status()
        );
    }

    @Test
    void rollout_rollbackPreservesProgress() {
        var rollout = Rollout.of(
            "ro-1", DEP_ID, "r1",
            DeploymentStrategy.CANARY
        );

        rollout = rollout.advance(75);
        rollout = rollout.rollback();

        assertEquals(75, rollout.progress());
        assertEquals(
            RolloutStatus.ROLLED_BACK,
            rollout.status()
        );
    }

    @Test
    void rollout_defaultsToRolling() {
        var rollout = new Rollout(
            "ro-1", DEP_ID, "r1",
            null, null, 0, null
        );

        assertEquals(
            DeploymentStrategy.ROLLING,
            rollout.strategy()
        );
        assertEquals(
            RolloutStatus.CREATED,
            rollout.status()
        );
        assertNotNull(rollout.createdAt());
    }

    // ===== Configuration =====

    @Test
    void config_rejectsBlankId() {
        assertThrows(
            IllegalArgumentException.class,
            () -> Configuration.of(
                " ", "1.0", Map.of()
            )
        );
    }

    @Test
    void config_rejectsBlankVersion() {
        assertThrows(
            IllegalArgumentException.class,
            () -> Configuration.of(
                "cfg", " ", Map.of()
            )
        );
    }

    @Test
    void config_defensiveCopyEntries() {
        Map<String, String> entries = new HashMap<>();
        entries.put("a", "b");

        var cfg = Configuration.of(
            "cfg", "1.0", entries
        );

        entries.put("c", "d");
        assertEquals(1, cfg.entries().size());
    }

    @Test
    void config_unmodifiableEntries() {
        var cfg = Configuration.of(
            "cfg", "1.0", Map.of("a", "b")
        );

        assertThrows(
            UnsupportedOperationException.class,
            () -> cfg.entries().put("k", "v")
        );
    }

    @Test
    void config_nullEntriesDefaultsToEmpty() {
        var cfg = new Configuration(
            "cfg", "1.0",
            null, null, null
        );

        assertTrue(cfg.entries().isEmpty());
        assertNotNull(cfg.createdAt());
    }

    // ===== ResourceQuota =====

    @Test
    void quota_rejectsBlankId() {
        assertThrows(
            IllegalArgumentException.class,
            () -> ResourceQuota.of(
                " ", QuotaScope.TENANT
            )
        );
    }

    @Test
    void quota_rejectsNullScope() {
        assertThrows(
            IllegalArgumentException.class,
            () -> ResourceQuota.of(
                "q1", null
            )
        );
    }

    @Test
    void quota_exactLimitIsNotWithin() {
        var quota = new ResourceQuota(
            "q1", QuotaScope.TENANT,
            Map.of("cpu", 10L),
            Map.of("cpu", 10L)
        );

        // current < limit → false when equal
        assertFalse(
            quota.isWithinLimit("cpu")
        );
    }

    @Test
    void quota_zeroUsageIsWithinLimit() {
        var quota = new ResourceQuota(
            "q1", QuotaScope.TENANT,
            Map.of("cpu", 10L),
            Map.of("cpu", 0L)
        );

        assertTrue(
            quota.isWithinLimit("cpu")
        );
    }

    @Test
    void quota_defensiveCopyLimits() {
        Map<String, Long> limits = new HashMap<>();
        limits.put("cpu", 10L);

        var quota = new ResourceQuota(
            "q1", QuotaScope.TENANT,
            limits, Map.of()
        );

        limits.put("gpu", 5L);
        assertEquals(1, quota.limits().size());
    }

    @Test
    void quota_unmodifiableMaps() {
        var quota = ResourceQuota.of(
            "q1", QuotaScope.TENANT
        );

        assertThrows(
            UnsupportedOperationException.class,
            () -> quota.limits().put("k", 1L)
        );
        assertThrows(
            UnsupportedOperationException.class,
            () -> quota.used().put("k", 1L)
        );
    }

    // ===== RuntimePool =====

    @Test
    void pool_rejectsBlankId() {
        assertThrows(
            IllegalArgumentException.class,
            () -> RuntimePool.of(
                " ", DEP_ID
            )
        );
    }

    @Test
    void pool_rejectsNullDeployment() {
        assertThrows(
            IllegalArgumentException.class,
            () -> RuntimePool.of(
                "pool-1", null
            )
        );
    }

    @Test
    void pool_defensiveCopyInstances() {
        RuntimeInstance inst =
            InMemoryRuntimeInstance.of(
                "i1", DEP_ID, "r1"
            ).start();

        var list = new ArrayList<>(
            List.of(inst)
        );

        var pool = new RuntimePool(
            "pool-1", DEP_ID,
            list, 1, 5, Map.of()
        );

        list.add(
            InMemoryRuntimeInstance.of(
                "i2", DEP_ID, "r1"
            ).start()
        );
        assertEquals(1, pool.instances().size());
    }

    @Test
    void pool_unmodifiableInstances() {
        var pool = RuntimePool.of(
            "pool-1", DEP_ID
        );

        assertThrows(
            UnsupportedOperationException.class,
            () -> pool.instances().add(null)
        );
    }

    @Test
    void pool_unhealthyBelowMinReplicas() {
        var inst = InMemoryRuntimeInstance.of(
            "i1", DEP_ID, "r1"
        ).start();

        var pool = new RuntimePool(
            "pool-1", DEP_ID,
            List.of(inst), 2, 5, Map.of()
        );

        assertFalse(pool.isHealthy());
    }

    @Test
    void pool_healthyAtMinReplicas() {
        var inst1 = InMemoryRuntimeInstance.of(
            "i1", DEP_ID, "r1"
        ).start();
        var inst2 = InMemoryRuntimeInstance.of(
            "i2", DEP_ID, "r1"
        ).start();

        var pool = new RuntimePool(
            "pool-1", DEP_ID,
            List.of(inst1, inst2),
            2, 5, Map.of()
        );

        assertTrue(pool.isHealthy());
    }

    // ===== RuntimeHealth =====

    @Test
    void health_nullLivenessDefaultsToUnknown() {
        var h = new RuntimeHealth(
            null, HealthStatus.HEALTHY,
            0, null
        );

        assertEquals(
            HealthStatus.UNKNOWN,
            h.liveness()
        );
        assertFalse(h.isHealthy());
    }

    @Test
    void health_nullReadinessDefaultsToUnknown() {
        var h = new RuntimeHealth(
            HealthStatus.HEALTHY, null,
            0, null
        );

        assertEquals(
            HealthStatus.UNKNOWN,
            h.readiness()
        );
        assertFalse(h.isHealthy());
    }

    @Test
    void health_livenessOnlyNotHealthy() {
        var h = new RuntimeHealth(
            HealthStatus.HEALTHY,
            HealthStatus.UNHEALTHY,
            10, null
        );

        assertFalse(h.isHealthy());
    }

    // ===== RuntimePolicy =====

    @Test
    void policy_rejectsBlankId() {
        assertThrows(
            IllegalArgumentException.class,
            () -> RuntimePolicy.defaults(" ")
        );
    }

    @Test
    void policy_rejectsNullId() {
        assertThrows(
            IllegalArgumentException.class,
            () -> RuntimePolicy.defaults(null)
        );
    }

    @Test
    void policy_defensiveCopyLimits() {
        Map<String, Long> limits = new HashMap<>();
        limits.put("cpu", 10L);

        var policy = new RuntimePolicy(
            "p1", 10, null, 3,
            limits, null
        );

        limits.put("gpu", 5L);
        assertEquals(1, policy.resourceLimits().size());
    }

    @Test
    void policy_unmodifiableMaps() {
        var policy = RuntimePolicy.defaults("p1");

        assertThrows(
            UnsupportedOperationException.class,
            () -> policy.resourceLimits()
                .put("k", 1L)
        );
        assertThrows(
            UnsupportedOperationException.class,
            () -> policy.extensions()
                .put("k", "v")
        );
    }

    // ===== ControlPlaneEvent =====

    @Test
    void event_rejectsBlankId() {
        assertThrows(
            IllegalArgumentException.class,
            () -> ControlPlaneEvent.of(
                " ", "type", DEP_ID
            )
        );
    }

    @Test
    void event_rejectsBlankType() {
        assertThrows(
            IllegalArgumentException.class,
            () -> ControlPlaneEvent.of(
                "e1", " ", DEP_ID
            )
        );
    }

    @Test
    void event_defensiveCopyData() {
        Map<String, String> data = new HashMap<>();
        data.put("a", "b");

        var evt = new ControlPlaneEvent(
            "e1", "type", DEP_ID,
            null, data
        );

        data.put("c", "d");
        assertEquals(1, evt.data().size());
    }

    @Test
    void event_unmodifiableData() {
        var evt = ControlPlaneEvent.of(
            "e1", "type", DEP_ID
        );

        assertThrows(
            UnsupportedOperationException.class,
            () -> evt.data().put("k", "v")
        );
    }

    // ===== DeploymentSpec =====

    @Test
    void spec_defensiveCopyResources() {
        Map<String, String> res = new HashMap<>();
        res.put("cpu", "2");

        var spec = new DeploymentSpec(
            "1.0.0", null, 1, 1,
            null, null, res, null
        );

        res.put("gpu", "1");
        assertEquals(1, spec.resources().size());
    }

    @Test
    void spec_unmodifiableMaps() {
        var spec = DeploymentSpec.of("1.0.0");

        assertThrows(
            UnsupportedOperationException.class,
            () -> spec.resources().put("k", "v")
        );
        assertThrows(
            UnsupportedOperationException.class,
            () -> spec.extensions().put("k", "v")
        );
    }

    @Test
    void spec_rejectsBlankVersion() {
        assertThrows(
            IllegalArgumentException.class,
            () -> DeploymentSpec.of(" ")
        );
    }

    // ===== DeploymentRevision =====

    @Test
    void revision_rejectsBlankId() {
        assertThrows(
            IllegalArgumentException.class,
            () -> DeploymentRevision.of(
                " ", DEP_ID, "1.0.0"
            )
        );
    }

    @Test
    void revision_rejectsNullDeployment() {
        assertThrows(
            IllegalArgumentException.class,
            () -> DeploymentRevision.of(
                "r1", null, "1.0.0"
            )
        );
    }

    // ===== SecretReference =====

    @Test
    void secret_rejectsBlankProvider() {
        assertThrows(
            IllegalArgumentException.class,
            () -> SecretReference.of(
                " ", "key"
            )
        );
    }

    @Test
    void secret_rejectsBlankKey() {
        assertThrows(
            IllegalArgumentException.class,
            () -> SecretReference.of(
                "vault", " "
            )
        );
    }

    @Test
    void secret_equality() {
        var a = SecretReference.of(
            "vault", "key"
        );
        var b = SecretReference.of(
            "vault", "key"
        );

        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
    }

    // ===== DeploymentStatus exhaustive =====

    @Test
    void deploymentStatus_allIsAvailable() {
        assertFalse(
            DeploymentStatus.CREATED.isAvailable()
        );
        assertFalse(
            DeploymentStatus.VALIDATING.isAvailable()
        );
        assertTrue(
            DeploymentStatus.ACTIVE.isAvailable()
        );
        assertTrue(
            DeploymentStatus.SCALING.isAvailable()
        );
        assertFalse(
            DeploymentStatus.UPGRADING.isAvailable()
        );
        assertFalse(
            DeploymentStatus.ROLLING_BACK.isAvailable()
        );
        assertFalse(
            DeploymentStatus.DEGRADED.isAvailable()
        );
        assertFalse(
            DeploymentStatus.STOPPED.isAvailable()
        );
    }

    @Test
    void deploymentStatus_allIsTerminal() {
        assertFalse(DeploymentStatus.CREATED.isTerminal());
        assertFalse(DeploymentStatus.VALIDATING.isTerminal());
        assertFalse(DeploymentStatus.ACTIVE.isTerminal());
        assertFalse(DeploymentStatus.SCALING.isTerminal());
        assertFalse(DeploymentStatus.UPGRADING.isTerminal());
        assertFalse(DeploymentStatus.ROLLING_BACK.isTerminal());
        assertFalse(DeploymentStatus.DEGRADED.isTerminal());
        assertTrue(DeploymentStatus.STOPPED.isTerminal());
    }

    @Test
    void deploymentStatus_allIsDegraded() {
        assertFalse(DeploymentStatus.CREATED.isDegraded());
        assertFalse(DeploymentStatus.VALIDATING.isDegraded());
        assertFalse(DeploymentStatus.ACTIVE.isDegraded());
        assertFalse(DeploymentStatus.SCALING.isDegraded());
        assertFalse(DeploymentStatus.UPGRADING.isDegraded());
        assertTrue(DeploymentStatus.ROLLING_BACK.isDegraded());
        assertTrue(DeploymentStatus.DEGRADED.isDegraded());
        assertFalse(DeploymentStatus.STOPPED.isDegraded());
    }

    // ===== InstanceStatus exhaustive =====

    @Test
    void instanceStatus_allIsAvailable() {
        assertFalse(InstanceStatus.CREATED.isAvailable());
        assertFalse(InstanceStatus.STARTING.isAvailable());
        assertTrue(InstanceStatus.READY.isAvailable());
        assertTrue(InstanceStatus.RUNNING.isAvailable());
        assertFalse(InstanceStatus.DRAINING.isAvailable());
        assertFalse(InstanceStatus.STOPPED.isAvailable());
        assertFalse(InstanceStatus.FAILED.isAvailable());
        assertFalse(InstanceStatus.UNHEALTHY.isAvailable());
        assertFalse(InstanceStatus.UNKNOWN.isAvailable());
    }

    @Test
    void instanceStatus_allIsTerminal() {
        assertFalse(InstanceStatus.CREATED.isTerminal());
        assertFalse(InstanceStatus.STARTING.isTerminal());
        assertFalse(InstanceStatus.READY.isTerminal());
        assertFalse(InstanceStatus.RUNNING.isTerminal());
        assertFalse(InstanceStatus.DRAINING.isTerminal());
        assertTrue(InstanceStatus.STOPPED.isTerminal());
        assertTrue(InstanceStatus.FAILED.isTerminal());
        assertFalse(InstanceStatus.UNHEALTHY.isTerminal());
        assertFalse(InstanceStatus.UNKNOWN.isTerminal());
    }

    // ===== ReleaseStatus exhaustive =====

    @Test
    void releaseStatus_allIsLive() {
        assertFalse(ReleaseStatus.CREATED.isLive());
        assertFalse(ReleaseStatus.VALIDATING.isLive());
        assertFalse(ReleaseStatus.APPROVED.isLive());
        assertTrue(ReleaseStatus.DEPLOYING.isLive());
        assertTrue(ReleaseStatus.ACTIVE.isLive());
        assertFalse(ReleaseStatus.REJECTED.isLive());
        assertFalse(ReleaseStatus.FAILED.isLive());
        assertFalse(ReleaseStatus.ROLLED_BACK.isLive());
    }

    @Test
    void releaseStatus_allIsTerminal() {
        assertFalse(ReleaseStatus.CREATED.isTerminal());
        assertFalse(ReleaseStatus.VALIDATING.isTerminal());
        assertFalse(ReleaseStatus.APPROVED.isTerminal());
        assertFalse(ReleaseStatus.DEPLOYING.isTerminal());
        assertFalse(ReleaseStatus.ACTIVE.isTerminal());
        assertTrue(ReleaseStatus.REJECTED.isTerminal());
        assertTrue(ReleaseStatus.FAILED.isTerminal());
        assertTrue(ReleaseStatus.ROLLED_BACK.isTerminal());
    }

    // ===== Environment =====

    @Test
    void environment_rejectsNullId() {
        assertThrows(
            NullPointerException.class,
            () -> Environment.of(null)
        );
    }

    @Test
    void environmentId_rejectsBlank() {
        assertThrows(
            IllegalArgumentException.class,
            () -> new EnvironmentId("")
        );
    }

    @Test
    void environmentId_equality() {
        var a = new EnvironmentId("dev");
        var b = new EnvironmentId("dev");
        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
    }

    // ===== DeploymentId =====

    @Test
    void deploymentId_equality() {
        var a = new DeploymentId(
            "ns", "agent"
        );
        var b = new DeploymentId(
            "ns", "agent"
        );
        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
    }

    @Test
    void deploymentId_inequality() {
        var a = new DeploymentId(
            "ns", "agent-a"
        );
        var b = new DeploymentId(
            "ns", "agent-b"
        );
        assertNotEquals(a, b);
    }
}
