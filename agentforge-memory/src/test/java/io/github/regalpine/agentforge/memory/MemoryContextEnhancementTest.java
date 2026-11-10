package io.github.regalpine.agentforge.memory;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * X5 Memory & Context Engineering
 * enhancement tests.
 */
class MemoryContextEnhancementTest {

    // --- ContextPriority ---

    @Test
    void shouldDefineFourPriorityLevels() {
        assertEquals(
            4,
            ContextPriority.values().length
        );
        assertNotNull(
            ContextPriority.CRITICAL
        );
        assertNotNull(
            ContextPriority.HIGH
        );
        assertNotNull(
            ContextPriority.NORMAL
        );
        assertNotNull(
            ContextPriority.LOW
        );
    }

    @Test
    void shouldOrderPriorityByOrdinal() {
        assertTrue(
            ContextPriority.CRITICAL.ordinal()
                < ContextPriority.LOW.ordinal()
        );
        assertTrue(
            ContextPriority.HIGH.ordinal()
                < ContextPriority.NORMAL.ordinal()
        );
    }

    // --- MemoryLifecycle ---

    @Test
    void shouldCheckTerminalStates() {
        assertTrue(
            MemoryLifecycle.DELETED
                .isTerminal()
        );
        assertTrue(
            MemoryLifecycle.REJECTED
                .isTerminal()
        );
        assertTrue(
            MemoryLifecycle.INVALID
                .isTerminal()
        );
        assertTrue(
            MemoryLifecycle.REVOKED
                .isTerminal()
        );
        assertFalse(
            MemoryLifecycle.ACTIVE
                .isTerminal()
        );
        assertFalse(
            MemoryLifecycle.CANDIDATE
                .isTerminal()
        );
    }

    @Test
    void shouldCheckRetrievable() {
        assertTrue(
            MemoryLifecycle.ACTIVE
                .isRetrievable()
        );
        assertFalse(
            MemoryLifecycle.EXPIRED
                .isRetrievable()
        );
        assertFalse(
            MemoryLifecycle.CANDIDATE
                .isRetrievable()
        );
    }

    @Test
    void shouldValidateTransitions() {
        assertTrue(
            MemoryLifecycle.CANDIDATE
                .canTransitionTo(
                    MemoryLifecycle.VALIDATING
                )
        );
        assertTrue(
            MemoryLifecycle.VALIDATING
                .canTransitionTo(
                    MemoryLifecycle.ACCEPTED
                )
        );
        assertTrue(
            MemoryLifecycle.ACCEPTED
                .canTransitionTo(
                    MemoryLifecycle.ACTIVE
                )
        );
        assertTrue(
            MemoryLifecycle.ACTIVE
                .canTransitionTo(
                    MemoryLifecycle.EXPIRED
                )
        );
        assertTrue(
            MemoryLifecycle.EXPIRED
                .canTransitionTo(
                    MemoryLifecycle.ARCHIVED
                )
        );
        assertTrue(
            MemoryLifecycle.ARCHIVED
                .canTransitionTo(
                    MemoryLifecycle.DELETED
                )
        );
    }

    @Test
    void shouldRejectInvalidTransitions() {
        assertFalse(
            MemoryLifecycle.DELETED
                .canTransitionTo(
                    MemoryLifecycle.ACTIVE
                )
        );
        assertFalse(
            MemoryLifecycle.CANDIDATE
                .canTransitionTo(
                    MemoryLifecycle.ACTIVE
                )
        );
        assertFalse(
            MemoryLifecycle.REJECTED
                .canTransitionTo(
                    MemoryLifecycle.ACTIVE
                )
        );
    }

    // --- MemoryTrustSource ---

    @Test
    void shouldCheckSystemInstruction() {
        assertTrue(
            MemoryTrustSource.SYSTEM
                .canBeSystemInstruction()
        );
        assertTrue(
            MemoryTrustSource.GOVERNED_KNOWLEDGE
                .canBeSystemInstruction()
        );
        assertFalse(
            MemoryTrustSource.MODEL_GENERATED
                .canBeSystemInstruction()
        );
        assertFalse(
            MemoryTrustSource.AGENT
                .canBeSystemInstruction()
        );
    }

    @Test
    void shouldCheckHighTrust() {
        assertTrue(
            MemoryTrustSource.SYSTEM
                .isHighTrust()
        );
        assertTrue(
            MemoryTrustSource.USER
                .isHighTrust()
        );
        assertFalse(
            MemoryTrustSource.MODEL_GENERATED
                .isHighTrust()
        );
        assertFalse(
            MemoryTrustSource.UNKNOWN
                .isHighTrust()
        );
    }

    @Test
    void shouldRequirePolicyCheck() {
        assertTrue(
            MemoryTrustSource.MODEL_GENERATED
                .requiresPolicyCheck()
        );
        assertTrue(
            MemoryTrustSource.AGENT
                .requiresPolicyCheck()
        );
        assertFalse(
            MemoryTrustSource.SYSTEM
                .requiresPolicyCheck()
        );
    }

    // --- TokenBudget ---

    @Test
    void shouldCalculateAvailableTokens() {
        var budget =
            new TokenBudget(
                32_000, 4_000, 2_000
            );

        assertEquals(
            26_000,
            budget.availableForContext()
        );
    }

    @Test
    void shouldCheckFitsWithin() {
        var budget =
            TokenBudget.standard();

        assertTrue(
            budget.fitsWithin(10_000)
        );
        assertFalse(
            budget.fitsWithin(100_000)
        );
    }

    @Test
    void shouldCreateFactoryBudgets() {
        var small =
            TokenBudget.small();
        var standard =
            TokenBudget.standard();
        var large =
            TokenBudget.large();

        assertTrue(
            small.maxInputTokens()
                < standard.maxInputTokens()
        );
        assertTrue(
            standard.maxInputTokens()
                < large.maxInputTokens()
        );
    }

    @Test
    void shouldValidateBudget() {
        var budget =
            new TokenBudget(
                32_000, 4_000, 2_000
            );

        var valid = budget.validate(
            2_000, 20_000, 4_000
        );
        assertTrue(valid.withinBudget());

        var invalid = budget.validate(
            2_000, 30_000, 4_000
        );
        assertFalse(invalid.withinBudget());
    }

    @Test
    void shouldDefaultInvalidBudget() {
        var budget =
            new TokenBudget(0, -1, -1);

        assertEquals(
            32_000,
            budget.maxInputTokens()
        );
        assertEquals(
            4_000,
            budget.reservedOutputTokens()
        );
        assertEquals(
            2_000,
            budget.reservedSystemTokens()
        );
    }

    // --- ContextPolicy ---

    @Test
    void shouldCreatePermissivePolicy() {
        var policy =
            ContextPolicy.permissive();

        assertTrue(
            policy.isMemoryTypeAllowed(
                MemoryType.FACT
            )
        );
        assertTrue(
            policy.allowConversation()
        );
        assertTrue(
            policy.allowToolResults()
        );
    }

    @Test
    void shouldCreateRestrictivePolicy() {
        var policy =
            ContextPolicy.restrictive();

        assertTrue(
            policy.isMemoryTypeAllowed(
                MemoryType.FACT
            )
        );
        assertFalse(
            policy.isMemoryTypeAllowed(
                MemoryType.EXPERIENCE
            )
        );
        assertFalse(
            policy.allowConversation()
        );
        assertEquals(
            5,
            policy.maxMemoryCount()
        );
    }

    @Test
    void shouldCheckPriorityAllowed() {
        var policy =
            ContextPolicy.restrictive();

        assertTrue(
            policy.isPriorityAllowed(
                ContextPriority.CRITICAL
            )
        );
        assertFalse(
            policy.isPriorityAllowed(
                ContextPriority.LOW
            )
        );
    }

    // --- ContextSnapshot ---

    @Test
    void shouldBuildSnapshot() {
        var snapshot =
            ContextSnapshot.builder()
                .snapshotId("snap-1")
                .contextId("ctx-1")
                .executionId("exec-1")
                .modelId("gpt-4")
                .memoryIds(
                    List.of("m1", "m2")
                )
                .tokenCount(5000)
                .policyDecision("ALLOW")
                .assemblyStrategy("HYBRID")
                .build();

        assertEquals(
            "snap-1",
            snapshot.snapshotId()
        );
        assertEquals(
            2,
            snapshot.memoryIds().size()
        );
        assertEquals(
            5000,
            snapshot.tokenCount()
        );
    }

    @Test
    void shouldCreateSnapshotFromContext() {
        var segments = List.of(
            new ContextSegment(
                "s1",
                ContextSegmentType.MEMORY,
                "test memory",
                0,
                "memory:m1"
            )
        );

        var ctx = new ModelContext(
            segments, 0,
            ContextBudget.small(),
            null
        );

        var snapshot =
            ContextSnapshot.from(
                "snap-2", ctx
            );

        assertEquals(
            "snap-2",
            snapshot.snapshotId()
        );
        assertFalse(
            snapshot.memoryIds().isEmpty()
        );
    }

    // --- MemoryConflictDetector ---

    @Test
    void shouldDetectConflicts() {
        var detector =
            new MemoryConflictDetector();

        var r1 = MemoryRecord.builder()
            .memoryId("m1")
            .tenantId("t1")
            .subjectId("agent-1")
            .type(MemoryType.FACT)
            .scope(MemoryScope.AGENT)
            .content("API = /v1")
            .status(MemoryStatus.ACTIVE)
            .build();

        var r2 = MemoryRecord.builder()
            .memoryId("m2")
            .tenantId("t1")
            .subjectId("agent-1")
            .type(MemoryType.FACT)
            .scope(MemoryScope.AGENT)
            .content("API = /v2")
            .status(MemoryStatus.ACTIVE)
            .build();

        var conflicts =
            detector.detect(List.of(r1, r2));

        assertEquals(1, conflicts.size());
        assertEquals(
            2,
            conflicts.get(0).size()
        );
    }

    @Test
    void shouldNotDetectSameContent() {
        var detector =
            new MemoryConflictDetector();

        var r1 = MemoryRecord.builder()
            .memoryId("m1")
            .tenantId("t1")
            .subjectId("agent-1")
            .type(MemoryType.FACT)
            .scope(MemoryScope.AGENT)
            .content("same content")
            .status(MemoryStatus.ACTIVE)
            .build();

        var r2 = MemoryRecord.builder()
            .memoryId("m2")
            .tenantId("t1")
            .subjectId("agent-1")
            .type(MemoryType.FACT)
            .scope(MemoryScope.AGENT)
            .content("same content")
            .status(MemoryStatus.ACTIVE)
            .build();

        var conflicts =
            detector.detect(List.of(r1, r2));

        assertTrue(conflicts.isEmpty());
    }

    @Test
    void shouldResolveByNewest() {
        var detector =
            new MemoryConflictDetector();

        var older = MemoryRecord.builder()
            .memoryId("m1")
            .tenantId("t1")
            .subjectId("a1")
            .type(MemoryType.FACT)
            .scope(MemoryScope.AGENT)
            .content("old")
            .createdAt(
                Instant.now().minusSeconds(100)
            )
            .status(MemoryStatus.ACTIVE)
            .build();

        var newer = MemoryRecord.builder()
            .memoryId("m2")
            .tenantId("t1")
            .subjectId("a1")
            .type(MemoryType.FACT)
            .scope(MemoryScope.AGENT)
            .content("new")
            .createdAt(Instant.now())
            .status(MemoryStatus.ACTIVE)
            .build();

        var group =
            new MemoryConflictDetector
                .ConflictGroup(
                    "key",
                    List.of(older, newer)
                );

        var resolved = detector.resolve(
            group,
            MemoryConflictDetector
                .ResolutionStrategy.NEWEST_WINS
        );

        assertEquals(
            "m2",
            resolved.memoryId()
        );
    }

    // --- MemoryCompressor ---

    @Test
    void shouldCompressLongContent() {
        var compressor =
            new MemoryCompressor(100);

        String longContent =
            "a".repeat(500);

        var record = MemoryRecord.builder()
            .memoryId("m1")
            .tenantId("t1")
            .subjectId("a1")
            .type(MemoryType.FACT)
            .scope(MemoryScope.AGENT)
            .content(longContent)
            .status(MemoryStatus.ACTIVE)
            .build();

        var result =
            compressor.compress(record);

        assertTrue(
            result.wasCompressed()
        );
        assertTrue(
            result.compressedLength()
                < longContent.length()
        );
        assertTrue(
            result.compressedContent()
                .contains("...")
        );
    }

    @Test
    void shouldNotCompressShort() {
        var compressor =
            new MemoryCompressor(1000);

        var record = MemoryRecord.builder()
            .memoryId("m1")
            .tenantId("t1")
            .subjectId("a1")
            .type(MemoryType.FACT)
            .scope(MemoryScope.AGENT)
            .content("short")
            .status(MemoryStatus.ACTIVE)
            .build();

        var result =
            compressor.compress(record);

        assertFalse(
            result.wasCompressed()
        );
        assertEquals(
            "short",
            result.compressedContent()
        );
    }

    // --- ContextAssemblyPipeline ---

    @Test
    void shouldRunFullPipeline() {
        var store =
            new InMemoryMemoryStore();

        var mem = MemoryRecord.builder()
            .memoryId("mem-1")
            .tenantId("t1")
            .subjectId("agent-1")
            .type(MemoryType.FACT)
            .scope(MemoryScope.AGENT)
            .content("Java 17 is used")
            .status(MemoryStatus.ACTIVE)
            .build();

        store.save(mem);

        var retriever =
            new InMemoryMemoryRetriever(
                store,
                MemoryPolicy.permissive()
            );

        var pipeline =
            new ContextAssemblyPipeline(
                retriever,
                TokenBudget.small()
            );

        var request =
            ContextRequest.builder()
                .subjectId("agent-1")
                .instructions(
                    "You are a helper"
                )
                .stateText(
                    "Step 1 complete"
                )
                .observations(
                    List.of("obs1")
                )
                .memoryQuery(
                    new MemoryQuery(
                        "agent-1",
                        MemoryScope.AGENT,
                        null,
                        "Java",
                        5,
                        RetrievalStrategy.KEYWORD
                    )
                )
                .build();

        var result =
            pipeline.assemble(request);

        assertNotNull(result.context());
        assertEquals(
            10,
            result.stageCount()
        );
        assertTrue(result.valid());
        assertFalse(
            result.context().segments()
                .isEmpty()
        );
    }

    @Test
    void shouldRunPipelineWithoutMemory() {
        var pipeline =
            new ContextAssemblyPipeline(
                null,
                TokenBudget.small()
            );

        var request =
            ContextRequest.builder()
                .subjectId("agent-1")
                .instructions("Hello")
                .build();

        var result =
            pipeline.assemble(request);

        assertNotNull(result.context());
        assertTrue(
            result.stageLog().size() >= 10
        );
    }

    @Test
    void shouldLogAllTenStages() {
        var pipeline =
            new ContextAssemblyPipeline(
                null,
                TokenBudget.standard()
            );

        var request =
            ContextRequest.builder()
                .subjectId("agent-1")
                .instructions("test")
                .stateText("state")
                .build();

        var result =
            pipeline.assemble(request);

        var log = result.stageLog();
        assertTrue(
            log.get(0).startsWith("COLLECT")
        );
        assertTrue(
            log.get(1).startsWith("RETRIEVE")
        );
        assertTrue(
            log.get(2).startsWith("FILTER")
        );
        assertTrue(
            log.get(3).startsWith("RANK")
        );
        assertTrue(
            log.get(4).startsWith("DEDUPLICATE")
        );
        assertTrue(
            log.get(5).startsWith("COMPRESS")
        );
        assertTrue(
            log.get(6).startsWith("BUDGET")
        );
        assertTrue(
            log.get(7).startsWith("ASSEMBLE")
        );
        assertTrue(
            log.get(8).startsWith("VALIDATE")
        );
        assertTrue(
            log.get(9).startsWith("DELIVER")
        );
    }

    @Test
    void shouldRespectContextPolicy() {
        var store =
            new InMemoryMemoryStore();

        // Add EXPERIENCE memory
        var mem = MemoryRecord.builder()
            .memoryId("mem-exp")
            .tenantId("t1")
            .subjectId("agent-1")
            .type(MemoryType.EXPERIENCE)
            .scope(MemoryScope.AGENT)
            .content("past experience")
            .status(MemoryStatus.ACTIVE)
            .build();

        store.save(mem);

        var retriever =
            new InMemoryMemoryRetriever(
                store,
                MemoryPolicy.permissive()
            );

        // Restrictive policy only
        // allows FACT and SUMMARY
        var restrictive =
            new ContextPolicy(
                "strict",
                Set.of(MemoryType.FACT),
                10, 32_000,
                true, true, true, true,
                null
            );

        var pipeline =
            new ContextAssemblyPipeline(
                retriever,
                MemoryPolicy.permissive(),
                TokenBudget.standard(),
                restrictive,
                null
            );

        var request =
            ContextRequest.builder()
                .subjectId("agent-1")
                .instructions("test")
                .memoryQuery(
                    new MemoryQuery(
                        "agent-1",
                        MemoryScope.AGENT,
                        null, "",
                        10,
                        RetrievalStrategy.KEYWORD
                    )
                )
                .build();

        var result =
            pipeline.assemble(request);

        // EXPERIENCE should be filtered
        // out by context policy
        var memSegments =
            result.context().segmentsOfType(
                ContextSegmentType.MEMORY
            );

        assertTrue(
            memSegments.isEmpty()
        );
    }
}
