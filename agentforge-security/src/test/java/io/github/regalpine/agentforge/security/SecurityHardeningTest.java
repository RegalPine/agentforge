package io.github.regalpine.agentforge.security;

import io.github.regalpine.agentforge.security.audit.*;
import io.github.regalpine.agentforge.security.egress.*;
import io.github.regalpine.agentforge.security.supply.*;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Enterprise Security Hardening tests — AF-SEC-01.
 * Covers: Audit Chain (§57-59), Egress Control (§45-46),
 * SSRF Protection (§46), Supply Chain (§49-52).
 * AF-SEC-020, AF-SEC-021, AF-SEC-024, AF-SEC-036.
 */
class SecurityHardeningTest {

    // --- Audit Chain ---

    @Test
    void shouldStoreAndRetrieveAuditEvent() {
        var store = new InMemoryAuditStore();
        var chain = new AuditChain(store);

        chain.recordSuccess(
            "evt-1",
            AuditEvent.AuditAction.AUTHENTICATION_SUCCESS,
            "user-1", "tenant-1");

        assertEquals(1, store.count());
        var found = store.findById("evt-1");
        assertTrue(found.isPresent());
        assertEquals(AuditEvent.AuditAction.AUTHENTICATION_SUCCESS,
            found.get().action());
    }

    @Test
    void shouldRecordDenial() {
        var store = new InMemoryAuditStore();
        var chain = new AuditChain(store);

        chain.recordDenial(
            "evt-2",
            AuditEvent.AuditAction.AUTHORIZATION_DENIED,
            "user-2", "tenant-1",
            "INSUFFICIENT_PERMISSIONS");

        var found = store.findById("evt-2");
        assertTrue(found.isPresent());
        assertEquals(AuditEvent.AuditResult.DENIED,
            found.get().result());
    }

    @Test
    void shouldQueryByTenant() {
        var store = new InMemoryAuditStore();
        var chain = new AuditChain(store);

        chain.recordSuccess("e1",
            AuditEvent.AuditAction.AUTHENTICATION_SUCCESS,
            "u1", "tenant-A");
        chain.recordSuccess("e2",
            AuditEvent.AuditAction.AUTHENTICATION_SUCCESS,
            "u2", "tenant-B");
        chain.recordSuccess("e3",
            AuditEvent.AuditAction.CAPABILITY_INVOKED,
            "u1", "tenant-A");

        var tenantA = store.findByTenant("tenant-A", 100);
        assertEquals(2, tenantA.size());

        var tenantB = store.findByTenant("tenant-B", 100);
        assertEquals(1, tenantB.size());
    }

    @Test
    void shouldQueryByAction() {
        var store = new InMemoryAuditStore();
        var chain = new AuditChain(store);

        chain.recordSuccess("e1",
            AuditEvent.AuditAction.AUTHENTICATION_SUCCESS,
            "u1", "t1");
        chain.recordDenial("e2",
            AuditEvent.AuditAction.POLICY_DENIED,
            "u1", "t1", "RATE_LIMIT");

        var denied = store.findByAction(
            AuditEvent.AuditAction.POLICY_DENIED, 100);
        assertEquals(1, denied.size());
    }

    @Test
    void shouldPurgeOldEvents() {
        var store = new InMemoryAuditStore();

        // Store an event with old timestamp
        var oldEvent = new AuditEvent(
            "old-1", Instant.now().minus(Duration.ofDays(100)),
            "t1", "u1",
            AuditEvent.AuditAction.AUTHENTICATION_SUCCESS,
            null, null, null,
            AuditEvent.AuditResult.ALLOWED,
            null, null, null,
            AuditEvent.Severity.INFO, java.util.Map.of());

        store.store(oldEvent);

        var recentEvent = AuditEvent.success(
            "new-1",
            AuditEvent.AuditAction.AUTHENTICATION_SUCCESS,
            "u1", "t1");
        store.store(recentEvent);

        assertEquals(2, store.count());

        int purged = store.purgeOlderThan(
            Instant.now().minus(Duration.ofDays(30)));
        assertEquals(1, purged);
        assertEquals(1, store.count());
    }

    @Test
    void shouldCreateAuditRetention() {
        var retention = AuditRetention.defaults();
        assertEquals(Duration.ofDays(90),
            retention.defaultRetention());
        assertEquals(Duration.ofDays(365),
            retention.criticalRetention());
    }

    @Test
    void shouldGetRetentionForSeverity() {
        var retention = AuditRetention.defaults();

        assertEquals(Duration.ofDays(365),
            retention.retentionFor(AuditEvent.Severity.CRITICAL));
        assertEquals(Duration.ofDays(180),
            retention.retentionFor(AuditEvent.Severity.HIGH));
        assertEquals(Duration.ofDays(7),
            retention.retentionFor(AuditEvent.Severity.INFO));
    }

    // --- SSRF Protection ---

    @Test
    void shouldBlockLocalhost() {
        var protector = new SsrfProtector();

        var result = protector.validate(
            "http://localhost/admin");
        assertFalse(result.allowed());
        assertTrue(result.reason().contains("blocked"));
    }

    @Test
    void shouldBlockLoopback() {
        var protector = new SsrfProtector();

        var result = protector.validate(
            "http://127.0.0.1/api");
        assertFalse(result.allowed());
    }

    @Test
    void shouldBlockAwsMetadata() {
        var protector = new SsrfProtector();

        var result = protector.validate(
            "http://169.254.169.254/latest/meta-data/");
        assertFalse(result.allowed());
    }

    @Test
    void shouldBlockPrivateNetworks() {
        var protector = new SsrfProtector();

        assertFalse(protector.validate(
            "http://10.0.0.1/internal").allowed());
        assertFalse(protector.validate(
            "http://192.168.1.1/admin").allowed());
        assertFalse(protector.validate(
            "http://172.16.0.1/internal").allowed());
    }

    @Test
    void shouldAllowPublicHosts() {
        var protector = new SsrfProtector();

        var result = protector.validate(
            "https://api.example.com/v1");
        assertTrue(result.allowed());
    }

    @Test
    void shouldRespectAllowlist() {
        var protector = new SsrfProtector(
            Set.of(),
            Set.of("internal.corp"));

        var result = protector.validate(
            "http://internal.corp/api");
        assertTrue(result.allowed());
    }

    @Test
    void shouldBlockNullUrl() {
        var protector = new SsrfProtector();
        assertFalse(protector.validate(null).allowed());
        assertFalse(protector.validate("").allowed());
    }

    // --- Egress Policy ---

    @Test
    void shouldDenyByDefault() {
        var policy = EgressPolicy.denyAll();

        assertEquals(EgressPolicy.EgressDecision.DENY,
            policy.check("unknown.host.com"));
    }

    @Test
    void shouldAllowExplicitHosts() {
        var policy = EgressPolicy.withAllowed(
            Set.of("api.openai.com", "api.anthropic.com"));

        assertEquals(EgressPolicy.EgressDecision.ALLOW,
            policy.check("api.openai.com"));
        assertEquals(EgressPolicy.EgressDecision.DENY,
            policy.check("evil.com"));
    }

    @Test
    void shouldDenyDenylistEvenIfAllowed() {
        var policy = new EgressPolicy(
            EgressPolicy.EgressDecision.DENY,
            Set.of("api.company.com"),
            Set.of(),
            Set.of("api.company.com"),
            100);

        assertEquals(EgressPolicy.EgressDecision.DENY,
            policy.check("api.company.com"));
    }

    // --- Egress Control ---

    @Test
    void shouldCombineSsrfAndEgress() {
        var control = new EgressControl(
            new SsrfProtector(),
            EgressPolicy.withAllowed(
                Set.of("api.example.com")));

        // SSRF block
        var ssrfResult = control.check(
            "http://169.254.169.254/metadata");
        assertFalse(ssrfResult.allowed());
        assertTrue(ssrfResult.reason()
            .contains("SSRF_PROTECTION"));

        // Egress deny
        var deniedResult = control.check(
            "https://evil.com/malware");
        assertFalse(deniedResult.allowed());
        assertTrue(deniedResult.reason()
            .contains("EGRESS_POLICY"));

        // Allowed
        var allowedResult = control.check(
            "https://api.example.com/v1");
        assertTrue(allowedResult.allowed());
    }

    @Test
    void shouldDenyAllByDefault() {
        var control = new EgressControl();
        var result = control.check(
            "https://api.example.com/v1");
        assertFalse(result.allowed());
    }

    // --- Supply Chain ---

    @Test
    void shouldRegisterComponent() {
        var registry = new SbomRegistry();
        var component = new ComponentRecord(
            "dep-1", "jackson-core", "2.15.0",
            ComponentRecord.ComponentType.JAVA_DEPENDENCY,
            "maven-central", "abc123", "Apache-2.0",
            ComponentRecord.SecurityStatus.VERIFIED,
            null, java.util.Map.of());

        registry.register(component);

        assertEquals(1, registry.size());
        assertTrue(registry.get("dep-1").isPresent());
        assertFalse(registry.hasVulnerabilities());
    }

    @Test
    void shouldDetectVulnerabilities() {
        var registry = new SbomRegistry();

        registry.register(new ComponentRecord(
            "dep-1", "vulnerable-lib", "1.0.0",
            ComponentRecord.ComponentType.JAVA_DEPENDENCY,
            "maven-central", "abc", "MIT",
            ComponentRecord.SecurityStatus.VULNERABLE,
            null, java.util.Map.of()));

        assertTrue(registry.hasVulnerabilities());
        assertEquals(1, registry.findByStatus(
            ComponentRecord.SecurityStatus.VULNERABLE).size());
    }

    @Test
    void shouldFindByType() {
        var registry = new SbomRegistry();

        registry.register(new ComponentRecord(
            "dep-1", "lib-a", "1.0",
            ComponentRecord.ComponentType.JAVA_DEPENDENCY,
            "maven", "a1", "MIT",
            null, null, java.util.Map.of()));

        registry.register(new ComponentRecord(
            "mcp-1", "mcp-server", "2.0",
            ComponentRecord.ComponentType.MCP_SERVER,
            "local", "b2", "Apache-2.0",
            null, null, java.util.Map.of()));

        assertEquals(1, registry.findByType(
            ComponentRecord.ComponentType.JAVA_DEPENDENCY).size());
        assertEquals(1, registry.findByType(
            ComponentRecord.ComponentType.MCP_SERVER).size());
    }

    @Test
    void shouldVerifyChecksum() {
        var verifier = new ArtifactVerifier();
        byte[] data = "hello world".getBytes(
            StandardCharsets.UTF_8);

        String checksum = verifier.computeChecksum(data);
        assertNotNull(checksum);

        var result = verifier.verifyChecksum(data, checksum);
        assertTrue(result.verified());
    }

    @Test
    void shouldDetectChecksumMismatch() {
        var verifier = new ArtifactVerifier();
        byte[] data = "hello world".getBytes(
            StandardCharsets.UTF_8);

        var result = verifier.verifyChecksum(data, "wrong");
        assertFalse(result.verified());
        assertTrue(result.reason().contains("mismatch"));
    }

    @Test
    void shouldValidateComponent() {
        var verifier = new ArtifactVerifier();

        // Valid component
        var valid = new ComponentRecord(
            "dep-1", "lib", "1.0",
            ComponentRecord.ComponentType.JAVA_DEPENDENCY,
            "maven", "abc123", "MIT",
            ComponentRecord.SecurityStatus.VERIFIED,
            null, java.util.Map.of());

        assertTrue(verifier.validateComponent(valid).isEmpty());

        // Missing checksum
        var noChecksum = new ComponentRecord(
            "dep-2", "lib2", "1.0",
            ComponentRecord.ComponentType.JAVA_DEPENDENCY,
            "maven", null, "MIT",
            null, null, java.util.Map.of());

        assertFalse(verifier.validateComponent(noChecksum)
            .isEmpty());

        // Blocked component
        var blocked = new ComponentRecord(
            "dep-3", "lib3", "1.0",
            ComponentRecord.ComponentType.JAVA_DEPENDENCY,
            "maven", "abc", "MIT",
            ComponentRecord.SecurityStatus.BLOCKED,
            null, java.util.Map.of());

        assertTrue(verifier.validateComponent(blocked)
            .stream().anyMatch(s -> s.contains("blocked")));
    }
}
