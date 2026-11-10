package io.github.regalpine.agentforge.memory;


import io.github.regalpine.agentforge.security.DataClassification;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for AE-08 Memory core model:
 * MemoryType, MemoryScope, MemoryStatus,
 * MemorySource, MemoryConfidence,
 * MemoryImportance, MemoryRecord,
 * MemoryCandidate, MemoryProvenance.
 */
class MemoryCoreModelTest {

    // --- MemoryType ---

    @Test
    void shouldHaveAllMemoryTypes() {
        assertEquals(6, MemoryType.values().length);
        assertNotNull(MemoryType.valueOf("CONVERSATION"));
        assertNotNull(MemoryType.valueOf("FACT"));
        assertNotNull(MemoryType.valueOf("PREFERENCE"));
        assertNotNull(MemoryType.valueOf("TASK"));
        assertNotNull(MemoryType.valueOf("EXPERIENCE"));
        assertNotNull(MemoryType.valueOf("SUMMARY"));
    }

    // --- MemoryScope ---

    @Test
    void shouldHaveAllMemoryScopes() {
        assertEquals(5, MemoryScope.values().length);
        assertNotNull(MemoryScope.valueOf("EXECUTION"));
        assertNotNull(MemoryScope.valueOf("CONVERSATION"));
        assertNotNull(MemoryScope.valueOf("USER"));
        assertNotNull(MemoryScope.valueOf("AGENT"));
        assertNotNull(MemoryScope.valueOf("TENANT"));
    }

    // --- MemoryStatus ---

    @Test
    void shouldHaveAllMemoryStatuses() {
        assertEquals(5, MemoryStatus.values().length);
        assertNotNull(MemoryStatus.valueOf("CANDIDATE"));
        assertNotNull(MemoryStatus.valueOf("ACTIVE"));
        assertNotNull(MemoryStatus.valueOf("EXPIRED"));
        assertNotNull(MemoryStatus.valueOf("ARCHIVED"));
        assertNotNull(MemoryStatus.valueOf("DELETED"));
    }

    // --- MemorySource ---

    @Test
    void shouldHaveAllMemorySources() {
        assertEquals(6, MemorySource.values().length);
        assertNotNull(MemorySource.valueOf("USER_EXPLICIT"));
        assertNotNull(MemorySource.valueOf("AGENT_INFERRED"));
        assertNotNull(MemorySource.valueOf("TOOL_RESULT"));
        assertNotNull(MemorySource.valueOf("CAPABILITY_RESULT"));
        assertNotNull(MemorySource.valueOf("SYSTEM"));
        assertNotNull(MemorySource.valueOf("IMPORTED"));
    }

    // --- MemoryImportance ---

    @Test
    void shouldHaveWeightedImportance() {
        assertTrue(
            MemoryImportance.CRITICAL.weight()
                > MemoryImportance.HIGH.weight()
        );
        assertTrue(
            MemoryImportance.HIGH.weight()
                > MemoryImportance.NORMAL.weight()
        );
        assertTrue(
            MemoryImportance.NORMAL.weight()
                > MemoryImportance.LOW.weight()
        );
    }

    // --- MemoryConfidence ---

    @Test
    void shouldCreateConfidence() {
        var conf = new MemoryConfidence(
            0.85, ConfidenceBasis.USER_EXPLICIT_STATEMENT
        );
        assertEquals(0.85, conf.score());
        assertEquals(
            ConfidenceBasis.USER_EXPLICIT_STATEMENT,
            conf.basis()
        );
    }

    @Test
    void shouldRejectInvalidConfidenceScore() {
        assertThrows(
            IllegalArgumentException.class,
            () -> new MemoryConfidence(
                -0.1, ConfidenceBasis.MANUAL
            )
        );
        assertThrows(
            IllegalArgumentException.class,
            () -> new MemoryConfidence(
                1.1, ConfidenceBasis.MANUAL
            )
        );
    }

    @Test
    void shouldCreateFactoryConfidence() {
        assertEquals(
            0.9,
            MemoryConfidence.high(
                ConfidenceBasis.USER_EXPLICIT_STATEMENT
            ).score()
        );
        assertEquals(
            0.6,
            MemoryConfidence.medium(
                ConfidenceBasis.AGENT_INFERENCE
            ).score()
        );
        assertEquals(
            0.3,
            MemoryConfidence.low(
                ConfidenceBasis.AGENT_INFERENCE
            ).score()
        );
    }

    // --- MemoryProvenance ---

    @Test
    void shouldCreateProvenance() {
        var prov = new MemoryProvenance(
            "exec-1", "msg-1", "cap-1", "agent-1"
        );
        assertEquals("exec-1", prov.sourceExecutionId());
        assertEquals("agent-1", prov.createdBy());
    }

    @Test
    void shouldCreateEmptyProvenance() {
        var prov = MemoryProvenance.empty();
        assertEquals("", prov.sourceExecutionId());
        assertEquals("", prov.createdBy());
    }

    // --- MemoryRecord ---

    @Test
    void shouldBuildMemoryRecord() {
        var record = MemoryRecord.builder()
            .memoryId("mem-001")
            .tenantId("tenant-a")
            .subjectId("user-001")
            .type(MemoryType.PREFERENCE)
            .scope(MemoryScope.USER)
            .content("用户偏好简洁回答")
            .source(MemorySource.USER_EXPLICIT)
            .importance(MemoryImportance.HIGH)
            .build();

        assertEquals("mem-001", record.memoryId());
        assertEquals("tenant-a", record.tenantId());
        assertEquals(
            MemoryType.PREFERENCE, record.type()
        );
        assertEquals(MemoryScope.USER, record.scope());
        assertEquals(
            MemorySource.USER_EXPLICIT,
            record.source()
        );
        assertEquals(
            MemoryStatus.CANDIDATE,
            record.status()
        );
        assertEquals(0, record.version());
    }

    @Test
    void shouldRejectBlankContent() {
        assertThrows(
            IllegalArgumentException.class,
            () -> MemoryRecord.builder()
                .memoryId("mem-001")
                .tenantId("t")
                .subjectId("s")
                .type(MemoryType.FACT)
                .scope(MemoryScope.USER)
                .content("  ")
                .build()
        );
    }

    @Test
    void shouldTransitionStatus() {
        var record = MemoryRecord.builder()
            .memoryId("mem-001")
            .tenantId("t")
            .subjectId("s")
            .type(MemoryType.FACT)
            .scope(MemoryScope.USER)
            .content("fact")
            .status(MemoryStatus.CANDIDATE)
            .build();

        var active = record.withStatus(
            MemoryStatus.ACTIVE
        );
        assertEquals(
            MemoryStatus.ACTIVE, active.status()
        );
        assertEquals(
            "mem-001", active.memoryId()
        );
    }

    @Test
    void shouldIncrementVersion() {
        var record = MemoryRecord.builder()
            .memoryId("mem-001")
            .tenantId("t")
            .subjectId("s")
            .type(MemoryType.FACT)
            .scope(MemoryScope.USER)
            .content("fact")
            .version(3)
            .build();

        var updated = record.withVersion(4);
        assertEquals(4, updated.version());
    }

    @Test
    void shouldCheckExpiration() {
        var record = MemoryRecord.builder()
            .memoryId("mem-001")
            .tenantId("t")
            .subjectId("s")
            .type(MemoryType.TASK)
            .scope(MemoryScope.EXECUTION)
            .content("task info")
            .expiresAt(
                Instant.now().minus(1, ChronoUnit.HOURS)
            )
            .status(MemoryStatus.ACTIVE)
            .build();

        assertTrue(record.isExpired(Instant.now()));
        assertFalse(
            record.isRetrievable(Instant.now())
        );
    }

    @Test
    void shouldCheckRetrievable() {
        var record = MemoryRecord.builder()
            .memoryId("mem-001")
            .tenantId("t")
            .subjectId("s")
            .type(MemoryType.FACT)
            .scope(MemoryScope.USER)
            .content("fact")
            .status(MemoryStatus.ACTIVE)
            .build();

        assertTrue(
            record.isRetrievable(Instant.now())
        );
    }

    @Test
    void shouldNotBeRetrievableWhenExpired() {
        var record = MemoryRecord.builder()
            .memoryId("mem-001")
            .tenantId("t")
            .subjectId("s")
            .type(MemoryType.FACT)
            .scope(MemoryScope.USER)
            .content("fact")
            .status(MemoryStatus.ACTIVE)
            .expiresAt(
                Instant.now().minusSeconds(60)
            )
            .build();

        assertFalse(
            record.isRetrievable(Instant.now())
        );
    }

    @Test
    void shouldNotBeRetrievableWhenNotActive() {
        var record = MemoryRecord.builder()
            .memoryId("mem-001")
            .tenantId("t")
            .subjectId("s")
            .type(MemoryType.FACT)
            .scope(MemoryScope.USER)
            .content("fact")
            .status(MemoryStatus.CANDIDATE)
            .build();

        assertFalse(
            record.isRetrievable(Instant.now())
        );
    }

    @Test
    void shouldDefaultClassification() {
        var record = MemoryRecord.builder()
            .memoryId("mem-001")
            .tenantId("t")
            .subjectId("s")
            .type(MemoryType.FACT)
            .scope(MemoryScope.USER)
            .content("fact")
            .build();

        assertEquals(
            DataClassification.INTERNAL,
            record.classification()
        );
    }

    // --- MemoryCandidate ---

    @Test
    void shouldCreateCandidate() {
        var candidate = new MemoryCandidate(
            "tenant-a",
            "user-001",
            MemoryType.PREFERENCE,
            MemoryScope.USER,
            "用户偏好简洁回答",
            null,
            MemorySource.USER_EXPLICIT,
            null,
            null,
            null,
            null
        );

        assertEquals(
            "tenant-a", candidate.tenantId()
        );
        assertEquals(
            MemorySource.USER_EXPLICIT,
            candidate.source()
        );
    }

    @Test
    void shouldRejectBlankCandidateContent() {
        assertThrows(
            IllegalArgumentException.class,
            () -> new MemoryCandidate(
                "t", "s",
                MemoryType.FACT,
                MemoryScope.USER,
                "",
                null, null, null, null, null, null
            )
        );
    }

    @Test
    void shouldDefaultAgentInferredConfidence() {
        var candidate = new MemoryCandidate(
            "t", "s",
            MemoryType.FACT,
            MemoryScope.AGENT,
            "inferred fact",
            null,
            MemorySource.AGENT_INFERRED,
            null, null, null, null
        );

        assertTrue(
            candidate.confidence().score() <= 0.5
        );
    }
}
