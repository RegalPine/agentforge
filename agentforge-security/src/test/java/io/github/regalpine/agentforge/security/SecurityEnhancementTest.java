package io.github.regalpine.agentforge.security;

import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class SecurityEnhancementTest {

    // --- InstructionTrustLevel ---

    @Test
    void systemShouldBeHighestTrust() {
        assertTrue(InstructionTrustLevel.SYSTEM.isHigherThan(InstructionTrustLevel.EXTERNAL));
        assertTrue(InstructionTrustLevel.SYSTEM.isHigherThan(InstructionTrustLevel.USER));
        assertTrue(InstructionTrustLevel.SYSTEM.isHigherThan(InstructionTrustLevel.TOOL));
    }

    @Test
    void externalShouldBeLowestTrust() {
        assertFalse(InstructionTrustLevel.EXTERNAL.isHigherThan(InstructionTrustLevel.SYSTEM));
        assertFalse(InstructionTrustLevel.EXTERNAL.isHigherThan(InstructionTrustLevel.USER));
    }

    @Test
    void trustHierarchyShouldBeOrdered() {
        InstructionTrustLevel[] levels = InstructionTrustLevel.values();
        for (int i = 0; i < levels.length - 1; i++) {
            assertTrue(levels[i].isHigherThan(levels[i + 1]),
                levels[i] + " should be higher than " + levels[i + 1]);
        }
    }

    // --- TrustInstruction ---

    @Test
    void shouldCreateTrustInstructions() {
        TrustInstruction sys = TrustInstruction.system("You are a helpful assistant.");
        assertEquals(InstructionTrustLevel.SYSTEM, sys.trustLevel());

        TrustInstruction ext = TrustInstruction.external("Web page content");
        assertEquals(InstructionTrustLevel.EXTERNAL, ext.trustLevel());
    }

    // --- SecretRedactor ---

    @Test
    void shouldRedactBearerToken() {
        SecretRedactor redactor = new SecretRedactor();
        String result = redactor.redact("Authorization: Bearer eyJhbGciOiJSUzI1NiJ9.test");
        assertTrue(result.contains("[REDACTED]"));
        assertFalse(result.contains("eyJhbGciOiJSUzI1NiJ9"));
    }

    @Test
    void shouldRedactApiKey() {
        SecretRedactor redactor = new SecretRedactor();
        String result = redactor.redact("api_key=sk-1234567890abcdef");
        assertTrue(result.contains("[REDACTED]"));
        assertFalse(result.contains("sk-1234567890abcdef"));
    }

    @Test
    void shouldRedactPasswordInJson() {
        SecretRedactor redactor = new SecretRedactor();
        String result = redactor.redact("{\"password\": \"my-secret-pass\"}");
        assertFalse(result.contains("my-secret-pass"));
    }

    @Test
    void shouldRedactSensitiveMapKeys() {
        SecretRedactor redactor = new SecretRedactor();
        Map<String, Object> result = redactor.redactMap(Map.of(
            "username", "alice",
            "api_key", "secret-key-123",
            "Authorization", "Bearer token-xyz"
        ));
        assertEquals("alice", result.get("username"));
        assertEquals("[REDACTED]", result.get("api_key"));
        assertEquals("[REDACTED]", result.get("Authorization"));
    }

    @Test
    void shouldDetectSensitiveData() {
        SecretRedactor redactor = new SecretRedactor();
        assertTrue(redactor.containsSensitiveData("Bearer abc123"));
        assertTrue(redactor.containsSensitiveData("password=secret"));
        assertFalse(redactor.containsSensitiveData("Hello world"));
        assertFalse(redactor.containsSensitiveData(null));
    }

    @Test
    void shouldHandleNullAndEmpty() {
        SecretRedactor redactor = new SecretRedactor();
        assertNull(redactor.redact(null));
        assertEquals("", redactor.redact(""));
    }

    // --- AuditEvent ---

    @Test
    void shouldCreateSuccessAuditEvent() {
        AuditEvent event = AuditEvent.success(
            UUID.randomUUID().toString(),
            AuditEvent.AuditAction.AUTHENTICATION_SUCCESS,
            "user-1",
            "tenant-1"
        );
        assertEquals(AuditEvent.AuditResult.ALLOWED, event.result());
        assertEquals(AuditEvent.Severity.INFO, event.severity());
        assertEquals("user-1", event.principalId());
    }

    @Test
    void shouldCreateDeniedAuditEvent() {
        AuditEvent event = AuditEvent.denied(
            UUID.randomUUID().toString(),
            AuditEvent.AuditAction.CAPABILITY_DENIED,
            "user-2",
            "tenant-1",
            "INSUFFICIENT_PERMISSIONS"
        );
        assertEquals(AuditEvent.AuditResult.DENIED, event.result());
        assertEquals("INSUFFICIENT_PERMISSIONS", event.reasonCode());
    }

    // --- AuditLog ---

    @Test
    void inMemoryAuditLogShouldRecordAndRetrieve() {
        AuditLog log = AuditLog.inMemory();
        AuditEvent e1 = AuditEvent.success("e1", AuditEvent.AuditAction.AUTHENTICATION_SUCCESS, "u1", "t1");
        AuditEvent e2 = AuditEvent.success("e2", AuditEvent.AuditAction.CAPABILITY_INVOKED, "u1", "t1");
        log.record(e1);
        log.record(e2);

        var recent = log.recent(10);
        assertEquals(2, recent.size());
        assertEquals("e2", recent.get(0).eventId()); // most recent first
    }

    // --- SecurityExecutionChain ---

    @Test
    void permissiveChainShouldAllowAll() {
        SecurityExecutionChain chain = SecurityExecutionChain.permissive();
        SecurityChainResult result = chain.execute(
            SecurityChainRequest.authenticate("user-1", "tenant-1")
        );
        assertTrue(result.allowed());
    }

    @Test
    void chainShouldDenyWhenCheckFails() {
        SecurityExecutionChain chain = SecurityExecutionChain.builder()
            .addCheck(req -> SecurityCheckResult.deny("NO_AUTH", "Not authenticated"))
            .build();

        SecurityChainResult result = chain.execute(
            SecurityChainRequest.authorize("user-1", "tenant-1", "agent", "agent-1")
        );
        assertTrue(result.isDenied());
        assertEquals("NO_AUTH", result.reasonCode());
    }

    @Test
    void chainShouldRecordAuditEvents() {
        AuditLog log = AuditLog.inMemory();
        SecurityExecutionChain chain = SecurityExecutionChain.builder()
            .auditLog(log)
            .addCheck(req -> SecurityCheckResult.allow())
            .build();

        chain.execute(SecurityChainRequest.authenticate("user-1", "tenant-1"));
        assertEquals(1, log.recent(10).size());
    }

    @Test
    void chainShouldRecordDeniedAuditEvents() {
        AuditLog log = AuditLog.inMemory();
        SecurityExecutionChain chain = SecurityExecutionChain.builder()
            .auditLog(log)
            .addCheck(req -> SecurityCheckResult.deny("DENIED", "no permission"))
            .build();

        chain.execute(SecurityChainRequest.invokeCapability("user-1", "tenant-1", "cap-1"));
        var events = log.recent(10);
        assertEquals(1, events.size());
        assertEquals(AuditEvent.AuditResult.DENIED, events.get(0).result());
    }

    // --- DataClassification ---

    @Test
    void restrictedShouldExceedConfidential() {
        assertTrue(DataClassification.RESTRICTED.exceeds(DataClassification.CONFIDENTIAL));
        assertTrue(DataClassification.RESTRICTED.exceeds(DataClassification.PUBLIC));
        assertFalse(DataClassification.PUBLIC.exceeds(DataClassification.RESTRICTED));
    }
}
