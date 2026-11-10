package io.github.regalpine.agentforge.hardening.conformance;

import io.github.regalpine.agentforge.controlplane.*;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * conformance-control-plane:
 * Deployment / RuntimeInstance /
 * Environment / Release.
 * AF-AD-16 §9.1, T-X12.14.
 */
class ControlPlaneConformanceTest {

    // ── DeploymentId ──

    @Test
    void afCp001_deploymentId() {
        var id = new DeploymentId(
            "prod", "research-agent"
        );
        assertEquals("prod",
            id.namespace());
        assertEquals(
            "research-agent",
            id.name()
        );
        assertEquals(
            "prod/research-agent",
            id.qualifiedName()
        );
    }

    // ── DeploymentStatus ──

    @Test
    void afCp002_eightStatuses() {
        assertEquals(
            8,
            DeploymentStatus.values()
                .length
        );
        assertTrue(
            DeploymentStatus.ACTIVE
                .isAvailable()
        );
        assertTrue(
            DeploymentStatus.STOPPED
                .isTerminal()
        );
        assertTrue(
            DeploymentStatus.DEGRADED
                .isDegraded()
        );
    }

    // ── DeploymentStrategy ──

    @Test
    void afCp003_fourStrategies() {
        assertEquals(
            4,
            DeploymentStrategy.values()
                .length
        );
        assertNotNull(
            DeploymentStrategy.ROLLING
        );
        assertNotNull(
            DeploymentStrategy.CANARY
        );
        assertNotNull(
            DeploymentStrategy.BLUE_GREEN
        );
    }

    // ── DeploymentSpec ──

    @Test
    void afCp004_specCreation() {
        var spec = DeploymentSpec.of(
            "1.0.0"
        );
        assertEquals("1.0.0",
            spec.agentVersion());
        assertEquals(1,
            spec.minReplicas());
    }

    // ── InstanceStatus ──

    @Test
    void afCp005_nineInstanceStatuses() {
        assertEquals(
            9,
            InstanceStatus.values().length
        );
        assertTrue(
            InstanceStatus.READY
                .isAvailable()
        );
        assertTrue(
            InstanceStatus.FAILED
                .isTerminal()
        );
        assertTrue(
            InstanceStatus.DRAINING
                .isDraining()
        );
    }

    // ── EnvironmentId ──

    @Test
    void afCp006_standardEnvs() {
        assertNotNull(
            EnvironmentId.DEV
        );
        assertNotNull(
            EnvironmentId.TEST
        );
        assertNotNull(
            EnvironmentId.STAGING
        );
        assertNotNull(
            EnvironmentId.PRODUCTION
        );
    }

    // ── Environment ──

    @Test
    void afCp007_environmentCreation() {
        var env = Environment.of(
            EnvironmentId.PRODUCTION
        );
        assertEquals(
            EnvironmentId.PRODUCTION,
            env.envId()
        );
        assertEquals(
            "production",
            env.type()
        );
    }

    // ── Configuration ──

    @Test
    void afCp008_versionedConfig() {
        var cfg = new Configuration(
            "cfg-1", "1.0.0",
            Map.of("key", "val"),
            "abc123", null
        );
        assertEquals("cfg-1",
            cfg.configId());
        assertEquals("1.0.0",
            cfg.version());
        assertNotNull(
            cfg.createdAt()
        );
    }

    // ── SecretReference ──

    @Test
    void afCp009_secretRef() {
        var ref = SecretReference.of(
            "vault", "api-key"
        );
        assertEquals("vault",
            ref.provider());
        assertEquals("api-key",
            ref.key());
    }

    // ── ResourceQuota ──

    @Test
    void afCp010_quotaCheck() {
        var quota = new ResourceQuota(
            "q1", QuotaScope.TENANT,
            Map.of("cpu", 100L),
            Map.of("cpu", 50L)
        );
        assertTrue(
            quota.isWithinLimit("cpu")
        );
    }

    // ── Rollout ──

    @Test
    void afCp011_rolloutCreation() {
        var r = new Rollout(
            "r1",
            new DeploymentId(
                "prod", "agent"),
            "rev-1",
            DeploymentStrategy.CANARY,
            RolloutStatus.CREATED,
            0, null
        );
        assertEquals("r1",
            r.rolloutId());
        assertEquals(
            RolloutStatus.CREATED,
            r.status()
        );
    }

    // ── Release ──

    @Test
    void afCp012_releaseLifecycle() {
        assertEquals(
            8,
            ReleaseStatus.values().length
        );
        assertTrue(
            ReleaseStatus.ACTIVE.isLive()
        );
        assertTrue(
            ReleaseStatus.FAILED
                .isTerminal()
        );
    }

    // ── HealthStatus ──

    @Test
    void afCp013_healthStatus() {
        assertEquals(
            4,
            HealthStatus.values().length
        );
        var h = RuntimeHealth.unknown();
        assertFalse(
            h.isHealthy()
        );
    }

    // ── RuntimePolicy ──

    @Test
    void afCp014_runtimePolicy() {
        var p = RuntimePolicy
            .defaults("p1");
        assertEquals("p1",
            p.policyId());
        assertTrue(
            p.maxRetries() >= 0
        );
    }

    // ── InMemoryDeployment ──

    @Test
    void afCp015_inMemoryDeployment() {
        var id = new DeploymentId(
            "test", "agent"
        );
        var spec = DeploymentSpec.of(
            "1.0.0"
        );
        var dep = new InMemoryDeployment(
            id, "agent-ref",
            EnvironmentId.DEV,
            spec,
            DeploymentStatus.CREATED,
            List.of(), null,
            Map.of()
        );
        assertEquals(id, dep.id());
        assertEquals(
            DeploymentStatus.CREATED,
            dep.status()
        );
    }
}
