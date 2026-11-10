package io.github.regalpine.agentforge.memory;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for AE-08 Memory Store, Retrieval,
 * Write Pipeline, Policy, Lifecycle,
 * Context Builder, Events, Metrics.
 */
class MemoryEnhancementTest {

    private InMemoryMemoryStore store;
    private MemoryPolicy policy;
    private CollectingMemoryEventSink eventSink;

    @BeforeEach
    void setUp() {
        store = new InMemoryMemoryStore();
        policy = MemoryPolicy.permissive();
        eventSink = new CollectingMemoryEventSink();
    }

    // --- InMemoryMemoryStore ---

    @Test
    void shouldSaveAndRetrieve() {
        var record = createRecord(
            "mem-1", "user-1",
            MemoryType.FACT, "Java 17 is LTS"
        );

        store.save(record);

        Optional<MemoryRecord> found =
            store.get("mem-1");

        assertTrue(found.isPresent());
        assertEquals(
            "mem-1",
            found.get().memoryId()
        );
    }

    @Test
    void shouldReturnEmptyForMissing() {
        assertTrue(
            store.get("nonexistent").isEmpty()
        );
    }

    @Test
    void shouldUpdate() {
        var record = createRecord(
            "mem-1", "user-1",
            MemoryType.FACT, "original"
        );
        store.save(record);

        var updated = record.withVersion(1);
        store.update(updated);

        var found = store.get("mem-1").orElseThrow();
        assertEquals(1, found.version());
    }

    @Test
    void shouldSoftDelete() {
        var record = createRecord(
            "mem-1", "user-1",
            MemoryType.FACT, "to delete"
        );
        store.save(record);

        store.delete("mem-1");

        var found = store.get("mem-1").orElseThrow();
        assertEquals(
            MemoryStatus.DELETED,
            found.status()
        );
    }

    @Test
    void shouldCheckExistence() {
        store.save(
            createRecord(
                "mem-1", "user-1",
                MemoryType.FACT, "exists"
            )
        );

        assertTrue(store.exists("mem-1"));
        assertFalse(store.exists("mem-2"));
    }

    // --- InMemoryMemoryRetriever ---

    @Test
    void shouldRetrieveByKeyword() {
        store.save(
            createRecord(
                "mem-1", "user-1",
                MemoryType.PREFERENCE,
                "用户偏好简洁回答"
            )
        );
        store.save(
            createRecord(
                "mem-2", "user-1",
                MemoryType.FACT,
                "Java 17 是 LTS 版本"
            )
        );

        var retriever =
            new InMemoryMemoryRetriever(
                store, policy
            );

        var query = new MemoryQuery(
            "user-1",
            MemoryScope.USER,
            null,
            "简洁",
            10,
            RetrievalStrategy.KEYWORD
        );

        var results = retriever.retrieve(query);
        assertEquals(1, results.size());
        assertEquals("mem-1", results.get(0).memoryId());
    }

    @Test
    void shouldRetrieveByExactMatch() {
        store.save(
            createRecord(
                "mem-1", "user-1",
                MemoryType.FACT,
                "exact content"
            )
        );

        var retriever =
            new InMemoryMemoryRetriever(
                store, policy
            );

        var query = new MemoryQuery(
            "user-1",
            MemoryScope.USER,
            MemoryType.FACT,
            "exact content",
            10,
            RetrievalStrategy.EXACT
        );

        var results = retriever.retrieve(query);
        assertEquals(1, results.size());
    }

    @Test
    void shouldNotRetrieveExpired() {
        var record = MemoryRecord.builder()
            .memoryId("mem-1")
            .tenantId("t")
            .subjectId("user-1")
            .type(MemoryType.FACT)
            .scope(MemoryScope.USER)
            .content("expired fact")
            .status(MemoryStatus.ACTIVE)
            .expiresAt(
                Instant.now().minus(1, ChronoUnit.HOURS)
            )
            .build();

        store.save(record);

        var retriever =
            new InMemoryMemoryRetriever(
                store, policy
            );

        var query = new MemoryQuery(
            "user-1",
            MemoryScope.USER,
            null, "",
            10,
            RetrievalStrategy.KEYWORD
        );

        var results = retriever.retrieve(query);
        assertTrue(results.isEmpty());
    }

    @Test
    void shouldNotRetrieveDeleted() {
        var record = createRecord(
            "mem-1", "user-1",
            MemoryType.FACT, "deleted fact"
        );
        store.save(record);
        store.delete("mem-1");

        var retriever =
            new InMemoryMemoryRetriever(
                store, policy
            );

        var query = new MemoryQuery(
            "user-1",
            MemoryScope.USER,
            null, "",
            10,
            RetrievalStrategy.KEYWORD
        );

        var results = retriever.retrieve(query);
        assertTrue(results.isEmpty());
    }

    @Test
    void shouldNotRetrieveCandidate() {
        var record = MemoryRecord.builder()
            .memoryId("mem-1")
            .tenantId("t")
            .subjectId("user-1")
            .type(MemoryType.FACT)
            .scope(MemoryScope.USER)
            .content("candidate fact")
            .status(MemoryStatus.CANDIDATE)
            .build();

        store.save(record);

        var retriever =
            new InMemoryMemoryRetriever(
                store, policy
            );

        var query = new MemoryQuery(
            "user-1",
            MemoryScope.USER,
            null, "",
            10,
            RetrievalStrategy.KEYWORD
        );

        var results = retriever.retrieve(query);
        assertTrue(results.isEmpty());
    }

    @Test
    void shouldRankByImportance() {
        store.save(
            MemoryRecord.builder()
                .memoryId("low")
                .tenantId("t")
                .subjectId("user-1")
                .type(MemoryType.FACT)
                .scope(MemoryScope.USER)
                .content("low importance")
                .status(MemoryStatus.ACTIVE)
                .importance(MemoryImportance.LOW)
                .build()
        );
        store.save(
            MemoryRecord.builder()
                .memoryId("high")
                .tenantId("t")
                .subjectId("user-1")
                .type(MemoryType.FACT)
                .scope(MemoryScope.USER)
                .content("high importance")
                .status(MemoryStatus.ACTIVE)
                .importance(MemoryImportance.HIGH)
                .build()
        );

        var retriever =
            new InMemoryMemoryRetriever(
                store, policy
            );

        var query = new MemoryQuery(
            "user-1",
            MemoryScope.USER,
            null, "",
            10,
            RetrievalStrategy.KEYWORD
        );

        var results = retriever.retrieve(query);
        assertEquals(2, results.size());
        assertEquals(
            "high",
            results.get(0).memoryId()
        );
    }

    @Test
    void shouldRespectLimit() {
        for (int i = 0; i < 5; i++) {
            store.save(
                createRecord(
                    "mem-" + i, "user-1",
                    MemoryType.FACT,
                    "fact " + i
                )
            );
        }

        var retriever =
            new InMemoryMemoryRetriever(
                store, policy
            );

        var query = new MemoryQuery(
            "user-1",
            MemoryScope.USER,
            null, "",
            3,
            RetrievalStrategy.KEYWORD
        );

        var results = retriever.retrieve(query);
        assertEquals(3, results.size());
    }

    @Test
    void shouldFilterByScope() {
        store.save(
            createRecord(
                "mem-1", "user-1",
                MemoryType.FACT, "user fact"
            )
        );
        store.save(
            createRecord(
                "mem-2", "user-2",
                MemoryType.FACT, "other user fact"
            )
        );

        var retriever =
            new InMemoryMemoryRetriever(
                store, policy
            );

        var query = new MemoryQuery(
            "user-1",
            MemoryScope.USER,
            null, "",
            10,
            RetrievalStrategy.KEYWORD
        );

        var results = retriever.retrieve(query);
        assertEquals(1, results.size());
        assertEquals("mem-1", results.get(0).memoryId());
    }

    @Test
    void shouldEmitRetrievalEvents() {
        var retriever =
            new InMemoryMemoryRetriever(
                store, policy, eventSink
            );

        var query = new MemoryQuery(
            "user-1",
            MemoryScope.USER,
            null, "",
            10,
            RetrievalStrategy.KEYWORD
        );

        retriever.retrieve(query);

        assertFalse(eventSink.events().isEmpty());
        assertFalse(
            eventSink.eventsOfType(
                MemoryEventType.MEMORY_RETRIEVAL_STARTED
            ).isEmpty()
        );
        assertFalse(
            eventSink.eventsOfType(
                MemoryEventType.MEMORY_RETRIEVAL_COMPLETED
            ).isEmpty()
        );
    }

    // --- MemoryWritePipeline ---

    @Test
    void shouldWriteCandidateThroughPipeline() {
        var pipeline =
            new MemoryWritePipeline(
                store, policy, eventSink
            );

        var candidate = new MemoryCandidate(
            "tenant-a",
            "user-1",
            MemoryType.PREFERENCE,
            MemoryScope.USER,
            "偏好简洁回答",
            null,
            MemorySource.USER_EXPLICIT,
            MemoryConfidence.high(
                ConfidenceBasis.USER_EXPLICIT_STATEMENT
            ),
            MemoryImportance.HIGH,
            null, null
        );

        var context = MemoryAccessContext.builder()
            .tenantId("tenant-a")
            .userId("user-1")
            .build();

        var result = pipeline.execute(
            candidate, context
        );

        assertTrue(result.success());
        assertTrue(result.record().isPresent());
        assertEquals(
            MemoryStatus.ACTIVE,
            result.record().get().status()
        );
        assertEquals(1, store.size());
    }

    @Test
    void shouldRejectWriteByPolicy() {
        var denyPolicy = MemoryPolicy.denyAll();

        var pipeline =
            new MemoryWritePipeline(
                store, denyPolicy
            );

        var candidate = new MemoryCandidate(
            "t", "s",
            MemoryType.FACT,
            MemoryScope.USER,
            "should be denied",
            null, null, null, null, null, null
        );

        var context = MemoryAccessContext.builder()
            .tenantId("t")
            .build();

        var result = pipeline.execute(
            candidate, context
        );

        assertTrue(result.isRejected());
        assertEquals(
            MemoryWriteResult.MemoryWriteStage.POLICY,
            result.rejectedAt()
        );
    }

    @Test
    void shouldRejectBlankContent() {
        // MemoryCandidate constructor rejects blank content
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
    void shouldEmitWriteEvents() {
        var pipeline =
            new MemoryWritePipeline(
                store, policy, eventSink
            );

        var candidate = new MemoryCandidate(
            "t", "s",
            MemoryType.FACT,
            MemoryScope.USER,
            "test content",
            null, null, null, null, null, null
        );

        var context = MemoryAccessContext.builder()
            .tenantId("t")
            .build();

        pipeline.execute(candidate, context);

        assertFalse(
            eventSink.eventsOfType(
                MemoryEventType.MEMORY_CANDIDATE_CREATED
            ).isEmpty()
        );
        assertFalse(
            eventSink.eventsOfType(
                MemoryEventType.MEMORY_WRITE_STARTED
            ).isEmpty()
        );
        assertFalse(
            eventSink.eventsOfType(
                MemoryEventType.MEMORY_WRITE_COMPLETED
            ).isEmpty()
        );
    }

    // --- MemoryPolicy ---

    @Test
    void shouldCreatePermissivePolicy() {
        var p = MemoryPolicy.permissive();
        var ctx = MemoryAccessContext.builder()
            .tenantId("t")
            .build();

        var query = new MemoryQuery(
            "s", MemoryScope.USER,
            null, "", 10
        );

        assertTrue(
            p.evaluateRead(query, ctx).allowed()
        );
    }

    @Test
    void shouldCreateDenyAllPolicy() {
        var p = MemoryPolicy.denyAll();
        var ctx = MemoryAccessContext.builder()
            .tenantId("t")
            .build();

        var query = new MemoryQuery(
            "s", MemoryScope.USER,
            null, "", 10
        );

        assertFalse(
            p.evaluateRead(query, ctx).allowed()
        );
    }

    @Test
    void shouldRestrictWriteByType() {
        var policy = TypeRestrictedMemoryPolicy.builder()
            .allowedWriteTypes(
                Set.of(
                    MemoryType.FACT,
                    MemoryType.PREFERENCE
                )
            )
            .build();

        var ctx = MemoryAccessContext.builder()
            .tenantId("t")
            .build();

        var allowed = new MemoryCandidate(
            "t", "s",
            MemoryType.FACT,
            MemoryScope.USER,
            "allowed",
            null,
            MemorySource.USER_EXPLICIT,
            null, null, null, null
        );

        var denied = new MemoryCandidate(
            "t", "s",
            MemoryType.EXPERIENCE,
            MemoryScope.USER,
            "denied",
            null,
            MemorySource.USER_EXPLICIT,
            null, null, null, null
        );

        assertTrue(
            policy.evaluateWrite(allowed, ctx).allowed()
        );
        assertFalse(
            policy.evaluateWrite(denied, ctx).allowed()
        );
    }

    @Test
    void shouldDenyAgentInferredUserMemory() {
        var policy = TypeRestrictedMemoryPolicy.builder()
            .build();

        var ctx = MemoryAccessContext.builder()
            .tenantId("t")
            .build();

        var candidate = new MemoryCandidate(
            "t", "s",
            MemoryType.PREFERENCE,
            MemoryScope.USER,
            "inferred preference",
            null,
            MemorySource.AGENT_INFERRED,
            null, null, null, null
        );

        var decision = policy.evaluateWrite(
            candidate, ctx
        );

        assertFalse(decision.allowed());
        assertEquals(
            MemoryDecision.MemoryDecisionType
                .DENY_REQUIRES_APPROVAL,
            decision.type()
        );
    }

    // --- Memory Lifecycle ---

    @Test
    void shouldTransitionLifecycle() {
        var record = MemoryRecord.builder()
            .memoryId("mem-1")
            .tenantId("t")
            .subjectId("s")
            .type(MemoryType.FACT)
            .scope(MemoryScope.USER)
            .content("lifecycle test")
            .status(MemoryStatus.CANDIDATE)
            .build();

        assertEquals(
            MemoryStatus.CANDIDATE,
            record.status()
        );

        var active = record.withStatus(
            MemoryStatus.ACTIVE
        );
        assertEquals(
            MemoryStatus.ACTIVE,
            active.status()
        );

        var expired = active.withStatus(
            MemoryStatus.EXPIRED
        );
        assertEquals(
            MemoryStatus.EXPIRED,
            expired.status()
        );

        var archived = expired.withStatus(
            MemoryStatus.ARCHIVED
        );
        assertEquals(
            MemoryStatus.ARCHIVED,
            archived.status()
        );
    }

    // --- ContextBudget ---

    @Test
    void shouldCreateDefaultBudget() {
        var budget = ContextBudget.defaults();
        assertEquals(32_000, budget.maxTokens());
        assertEquals(8_000, budget.memoryTokens());
        assertEquals(8_000, budget.observationTokens());
        assertEquals(4_000, budget.stateTokens());
        assertEquals(4_000, budget.instructionTokens());
        assertTrue(budget.outputTokens() > 0);
    }

    @Test
    void shouldCreateSmallBudget() {
        var budget = ContextBudget.small();
        assertEquals(4_000, budget.maxTokens());
        assertEquals(1_000, budget.memoryTokens());
    }

    // --- ContextSegment ---

    @Test
    void shouldCreateContextSegment() {
        var seg = new ContextSegment(
            "seg-1",
            ContextSegmentType.MEMORY,
            "memory content",
            3,
            "memory:mem-001"
        );

        assertEquals("seg-1", seg.segmentId());
        assertEquals(
            ContextSegmentType.MEMORY,
            seg.type()
        );
        assertTrue(seg.estimatedTokens() > 0);
    }

    // --- ContextBuilder ---

    @Test
    void shouldBuildContext() {
        store.save(
            createRecord(
                "mem-1", "user-1",
                MemoryType.PREFERENCE,
                "偏好简洁"
            )
        );

        var retriever =
            new InMemoryMemoryRetriever(
                store, policy
            );

        var builder = new ContextBuilder(
            retriever,
            ContextBudget.small()
        );

        var request = ContextRequest.builder()
            .subjectId("user-1")
            .instructions("你是助手")
            .stateText("当前步骤: 1/3")
            .observations(
                List.of("用户询问天气")
            )
            .memoryQuery(
                new MemoryQuery(
                    "user-1",
                    MemoryScope.USER,
                    null, "",
                    5,
                    RetrievalStrategy.KEYWORD
                )
            )
            .build();

        var context = builder.build(request);

        assertFalse(context.segments().isEmpty());
        assertTrue(context.totalTokens() > 0);

        var memSegs = context.segmentsOfType(
            ContextSegmentType.MEMORY
        );
        assertFalse(memSegs.isEmpty());
    }

    @Test
    void shouldBuildContextWithoutMemory() {
        var builder = new ContextBuilder(null);

        var request = ContextRequest.builder()
            .subjectId("user-1")
            .instructions("你是助手")
            .stateText("step 1")
            .build();

        var context = builder.build(request);

        assertFalse(context.segments().isEmpty());

        var instrSegs = context.segmentsOfType(
            ContextSegmentType.INSTRUCTION
        );
        assertEquals(1, instrSegs.size());
    }

    @Test
    void shouldEmitContextBuildEvents() {
        var retriever =
            new InMemoryMemoryRetriever(
                store, policy
            );

        var builder = new ContextBuilder(
            retriever,
            ContextBudget.small(),
            eventSink
        );

        var request = ContextRequest.builder()
            .subjectId("user-1")
            .instructions("test")
            .build();

        builder.build(request);

        assertFalse(
            eventSink.eventsOfType(
                MemoryEventType.CONTEXT_BUILD_STARTED
            ).isEmpty()
        );
        assertFalse(
            eventSink.eventsOfType(
                MemoryEventType.CONTEXT_BUILD_COMPLETED
            ).isEmpty()
        );
    }

    // --- ModelContext ---

    @Test
    void shouldGenerateFingerprint() {
        var seg = new ContextSegment(
            "seg-1",
            ContextSegmentType.SYSTEM,
            "system prompt",
            0,
            "system"
        );

        var ctx = new ModelContext(
            List.of(seg),
            0,
            ContextBudget.defaults(),
            null
        );

        assertFalse(ctx.fingerprint().isEmpty());
        assertTrue(
            ctx.fingerprint().contains("seg-1")
        );
    }

    @Test
    void shouldAssembleText() {
        var seg1 = new ContextSegment(
            "s1",
            ContextSegmentType.INSTRUCTION,
            "instruction",
            0, "sys"
        );
        var seg2 = new ContextSegment(
            "s2",
            ContextSegmentType.MEMORY,
            "memory",
            1, "mem"
        );

        var ctx = new ModelContext(
            List.of(seg1, seg2),
            0,
            ContextBudget.defaults(),
            null
        );

        assertEquals(
            "instruction\n\nmemory",
            ctx.assembledText()
        );
    }

    // --- MemoryMetrics ---

    @Test
    void shouldTrackMetrics() {
        var metrics = new MemoryMetrics();

        metrics.recordRead();
        metrics.recordRead();
        metrics.recordWrite();
        metrics.recordHit();
        metrics.recordMiss();
        metrics.recordCandidate();
        metrics.recordPromoted();
        metrics.recordRejected();
        metrics.recordExpired();
        metrics.recordConflict();
        metrics.recordPolicyDenied();
        metrics.recordContextBuild();

        assertEquals(2, metrics.readTotal());
        assertEquals(1, metrics.writeTotal());
        assertEquals(1, metrics.hitTotal());
        assertEquals(1, metrics.missTotal());
        assertEquals(1, metrics.candidateTotal());
        assertEquals(1, metrics.promotedTotal());
        assertEquals(1, metrics.rejectedTotal());
        assertEquals(1, metrics.expiredTotal());
        assertEquals(1, metrics.conflictTotal());
        assertEquals(1, metrics.policyDeniedTotal());
        assertEquals(1, metrics.contextBuildTotal());
    }

    @Test
    void shouldResetMetrics() {
        var metrics = new MemoryMetrics();
        metrics.recordRead();
        metrics.recordRead();

        assertEquals(2, metrics.readTotal());

        metrics.reset();
        assertEquals(0, metrics.readTotal());
    }

    // --- InMemoryMemoryProvider ---

    @Test
    void shouldCreateInMemoryProvider() {
        var provider = new InMemoryMemoryProvider();

        assertEquals(
            "inmemory",
            provider.providerType()
        );
        assertNotNull(provider.store());
        assertNotNull(provider.retriever());
        assertNotNull(provider.capabilities());
        assertFalse(
            provider.capabilities()
                .supportsSemanticRetrieval()
        );
    }

    @Test
    void shouldUseProviderEndToEnd() {
        var provider = new InMemoryMemoryProvider(
            policy, eventSink
        );

        // Write via pipeline
        var pipeline =
            new MemoryWritePipeline(
                provider.store(),
                policy,
                eventSink
            );

        var candidate = new MemoryCandidate(
            "t", "user-1",
            MemoryType.PREFERENCE,
            MemoryScope.USER,
            "偏好简洁",
            null,
            MemorySource.USER_EXPLICIT,
            null, null, null, null
        );

        var ctx = MemoryAccessContext.builder()
            .tenantId("t")
            .userId("user-1")
            .build();

        var writeResult = pipeline.execute(
            candidate, ctx
        );
        assertTrue(writeResult.success());

        // Read via retriever
        var query = new MemoryQuery(
            "user-1",
            MemoryScope.USER,
            null, "简洁",
            10,
            RetrievalStrategy.KEYWORD
        );

        var results =
            provider.retriever().retrieve(query);
        assertEquals(1, results.size());
    }

    // --- CollectingMemoryEventSink ---

    @Test
    void shouldCollectEvents() {
        var sink = new CollectingMemoryEventSink();

        sink.emit(
            new MemoryEvent(
                MemoryEventType.MEMORY_WRITE_STARTED,
                "user-1", 0
            )
        );
        sink.emit(
            new MemoryEvent(
                MemoryEventType.MEMORY_WRITE_COMPLETED,
                "user-1", 0
            )
        );

        assertEquals(2, sink.eventCount());
        assertEquals(
            1,
            sink.eventsOfType(
                MemoryEventType.MEMORY_WRITE_STARTED
            ).size()
        );
    }

    @Test
    void shouldClearEvents() {
        var sink = new CollectingMemoryEventSink();
        sink.emit(
            new MemoryEvent(
                MemoryEventType.MEMORY_EXPIRED,
                "s", 0
            )
        );
        assertEquals(1, sink.eventCount());

        sink.clear();
        assertEquals(0, sink.eventCount());
    }

    // --- MemoryDecision ---

    @Test
    void shouldCreateDecisions() {
        var allow = MemoryDecision.allow();
        assertTrue(allow.allowed());
        assertFalse(allow.isDenied());

        var deny = MemoryDecision.deny("reason");
        assertFalse(deny.allowed());
        assertTrue(deny.isDenied());
        assertEquals("reason", deny.reason());

        var redact = MemoryDecision.denyRedact(
            "sensitive"
        );
        assertEquals(
            MemoryDecision.MemoryDecisionType.DENY_REDACT,
            redact.type()
        );
    }

    // --- MemoryAccessContext ---

    @Test
    void shouldBuildAccessContext() {
        var ctx = MemoryAccessContext.builder()
            .executionId("exec-1")
            .tenantId("tenant-a")
            .agentId("agent-1")
            .userId("user-1")
            .conversationId("conv-1")
            .build();

        assertEquals("exec-1", ctx.executionId());
        assertEquals("tenant-a", ctx.tenantId());
        assertEquals("agent-1", ctx.agentId());
        assertEquals("user-1", ctx.userId());
    }

    @Test
    void shouldRequireTenantId() {
        assertThrows(
            NullPointerException.class,
            () -> MemoryAccessContext.builder()
                .executionId("e")
                .build()
        );
    }

    // --- MemoryEvent ---

    @Test
    void shouldCreateMemoryEvent() {
        var event = new MemoryEvent(
            MemoryEventType.MEMORY_EXPIRED,
            "user-1",
            System.currentTimeMillis()
        );

        assertEquals(
            MemoryEventType.MEMORY_EXPIRED,
            event.type()
        );
        assertEquals("user-1", event.subjectId());
        assertTrue(event.timestamp() > 0);
    }

    // --- RetrievalStrategy ---

    @Test
    void shouldHaveAllStrategies() {
        assertEquals(
            4,
            RetrievalStrategy.values().length
        );
        assertNotNull(
            RetrievalStrategy.valueOf("EXACT")
        );
        assertNotNull(
            RetrievalStrategy.valueOf("KEYWORD")
        );
        assertNotNull(
            RetrievalStrategy.valueOf("SEMANTIC")
        );
        assertNotNull(
            RetrievalStrategy.valueOf("HYBRID")
        );
    }

    // --- MemoryUpdateStrategy ---

    @Test
    void shouldHaveAllUpdateStrategies() {
        assertEquals(
            4,
            MemoryUpdateStrategy.values().length
        );
        assertNotNull(
            MemoryUpdateStrategy.valueOf("REPLACE")
        );
        assertNotNull(
            MemoryUpdateStrategy.valueOf("VERSION")
        );
    }

    // --- Helpers ---

    private MemoryRecord createRecord(
        String id,
        String subjectId,
        MemoryType type,
        String content
    ) {
        return MemoryRecord.builder()
            .memoryId(id)
            .tenantId("t")
            .subjectId(subjectId)
            .type(type)
            .scope(MemoryScope.USER)
            .content(content)
            .status(MemoryStatus.ACTIVE)
            .build();
    }
}
