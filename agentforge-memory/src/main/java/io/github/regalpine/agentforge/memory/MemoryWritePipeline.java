package io.github.regalpine.agentforge.memory;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Memory write pipeline.
 * AE-08 §14-15: Memory Write Pipeline.
 *
 * <p>Flow:
 * Candidate → Normalize → Validate →
 * Policy → Deduplicate → Persist →
 * Activate.
 *
 * <p>The pipeline ensures that no memory
 * is persisted without passing all stages.
 * LLM output can only produce candidates,
 * not direct memory records.
 */
public final class MemoryWritePipeline {

    private final MemoryStore store;
    private final MemoryPolicy policy;
    private final MemoryEventSink eventSink;

    public MemoryWritePipeline(
        MemoryStore store,
        MemoryPolicy policy,
        MemoryEventSink eventSink
    ) {
        this.store =
            Objects.requireNonNull(store, "store");
        this.policy =
            Objects.requireNonNull(policy, "policy");
        this.eventSink = eventSink;
    }

    public MemoryWritePipeline(
        MemoryStore store,
        MemoryPolicy policy
    ) {
        this(store, policy, null);
    }

    /**
     * Execute the full write pipeline
     * for a memory candidate.
     */
    public MemoryWriteResult execute(
        MemoryCandidate candidate,
        MemoryAccessContext context
    ) {
        Objects.requireNonNull(
            candidate, "candidate"
        );
        Objects.requireNonNull(
            context, "context"
        );

        emitEvent(
            MemoryEventType.MEMORY_CANDIDATE_CREATED,
            candidate.subjectId()
        );

        // Stage 1: Normalize
        MemoryCandidate normalized =
            normalize(candidate);

        emitEvent(
            MemoryEventType.MEMORY_WRITE_STARTED,
            normalized.subjectId()
        );

        // Stage 2: Validate
        if (!isValid(normalized)) {
            return MemoryWriteResult.rejected(
                MemoryWriteResult.MemoryWriteStage.VALIDATE,
                "Candidate validation failed"
            );
        }

        // Stage 3: Policy
        MemoryDecision writeDecision =
            policy.evaluateWrite(normalized, context);

        if (writeDecision.isDenied()) {
            emitEvent(
                MemoryEventType.MEMORY_WRITE_REJECTED,
                candidate.subjectId()
            );

            return MemoryWriteResult.rejected(
                MemoryWriteResult.MemoryWriteStage.POLICY,
                writeDecision.reason()
            );
        }

        // Stage 4: Deduplicate
        Optional<MemoryRecord> existing =
            findDuplicate(normalized);

        if (existing.isPresent()) {
            MemoryRecord updated =
                existing.get().withVersion(
                    existing.get().version() + 1
                );
            store.update(updated);

            emitEvent(
                MemoryEventType.MEMORY_WRITE_COMPLETED,
                updated.subjectId()
            );

            return MemoryWriteResult.success(updated);
        }

        // Stage 5: Persist
        MemoryRecord record = toRecord(normalized);
        MemoryRecord saved = store.save(record);

        // Stage 6: Activate
        MemoryRecord activated =
            saved.withStatus(MemoryStatus.ACTIVE);
        store.update(activated);

        emitEvent(
            MemoryEventType.MEMORY_WRITE_COMPLETED,
            activated.subjectId()
        );

        return MemoryWriteResult.success(activated);
    }

    private MemoryCandidate normalize(
        MemoryCandidate candidate
    ) {
        String trimmed =
            candidate.content().trim();

        return new MemoryCandidate(
            candidate.tenantId(),
            candidate.subjectId(),
            candidate.type(),
            candidate.scope(),
            trimmed,
            candidate.attributes(),
            candidate.source(),
            candidate.confidence(),
            candidate.importance(),
            candidate.provenance(),
            candidate.suggestedExpiresAt()
        );
    }

    private boolean isValid(
        MemoryCandidate candidate
    ) {
        if (candidate.content().isBlank()) {
            return false;
        }

        if (candidate.content().length() > 100_000) {
            return false;
        }

        return true;
    }

    private Optional<MemoryRecord> findDuplicate(
        MemoryCandidate candidate
    ) {
        return Optional.empty();
    }

    private MemoryRecord toRecord(
        MemoryCandidate candidate
    ) {
        String id =
            "mem-" + UUID.randomUUID();

        return MemoryRecord.builder()
            .memoryId(id)
            .tenantId(candidate.tenantId())
            .subjectId(candidate.subjectId())
            .type(candidate.type())
            .scope(candidate.scope())
            .content(candidate.content())
            .attributes(candidate.attributes())
            .source(candidate.source())
            .confidence(candidate.confidence())
            .importance(candidate.importance())
            .provenance(candidate.provenance())
            .expiresAt(candidate.suggestedExpiresAt())
            .status(MemoryStatus.CANDIDATE)
            .version(0)
            .build();
    }

    private void emitEvent(
        MemoryEventType type,
        String subjectId
    ) {
        if (eventSink != null) {
            eventSink.emit(
                new MemoryEvent(
                    type, subjectId,
                    System.currentTimeMillis()
                )
            );
        }
    }
}
