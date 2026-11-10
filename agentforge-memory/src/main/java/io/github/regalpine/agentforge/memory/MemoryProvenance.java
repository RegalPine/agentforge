package io.github.regalpine.agentforge.memory;

import java.util.Map;
import java.util.Objects;

/**
 * Memory provenance information.
 * AE-08 §37: Memory Provenance.
 *
 * <p>Records why and where a memory
 * was created so that the system can
 * answer "why does this memory exist?".
 */
public record MemoryProvenance(
    String sourceExecutionId,
    String sourceMessageId,
    String sourceCapabilityId,
    String createdBy
) {

    public MemoryProvenance {
        sourceExecutionId =
            sourceExecutionId == null
                ? "" : sourceExecutionId;
        sourceMessageId =
            sourceMessageId == null
                ? "" : sourceMessageId;
        sourceCapabilityId =
            sourceCapabilityId == null
                ? "" : sourceCapabilityId;
        createdBy =
            createdBy == null
                ? "" : createdBy;
    }

    public static MemoryProvenance empty() {
        return new MemoryProvenance(
            "", "", "", ""
        );
    }
}
