package io.github.regalpine.agentforge.memory;

import java.util.Objects;
import java.util.Optional;

/**
 * Memory write pipeline result.
 * AE-08 §14: Memory Write Pipeline.
 *
 * <p>Tracks the outcome of a write
 * attempt through the full pipeline:
 * Candidate → Normalize → Validate →
 * Policy → Deduplicate → Persist → Active.
 */
public record MemoryWriteResult(
    boolean success,
    Optional<MemoryRecord> record,
    MemoryWriteStage rejectedAt,
    String reason
) {

    public enum MemoryWriteStage {
        NORMALIZE,
        VALIDATE,
        POLICY,
        DEDUPLICATE,
        PERSIST,
        ACTIVATE
    }

    public static MemoryWriteResult success(
        MemoryRecord record
    ) {
        Objects.requireNonNull(record, "record");
        return new MemoryWriteResult(
            true,
            Optional.of(record),
            null,
            ""
        );
    }

    public static MemoryWriteResult rejected(
        MemoryWriteStage stage,
        String reason
    ) {
        return new MemoryWriteResult(
            false,
            Optional.empty(),
            stage,
            reason
        );
    }

    public boolean isRejected() {
        return !success;
    }
}
