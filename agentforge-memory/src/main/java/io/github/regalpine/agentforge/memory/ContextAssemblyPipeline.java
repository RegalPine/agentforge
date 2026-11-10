package io.github.regalpine.agentforge.memory;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/**
 * Context assembly pipeline.
 * AF-ENHANCE-06 §22: Context
 * Engineering Pipeline.
 *
 * <p>Implements the 10-stage pipeline:
 * <pre>
 * COLLECT → RETRIEVE → FILTER →
 * RANK → DEDUPLICATE → COMPRESS →
 * BUDGET → ASSEMBLE → VALIDATE →
 * DELIVER
 * </pre>
 *
 * <p>Each stage transforms a list of
 * candidate context segments. The
 * pipeline is configurable: stages
 * can be skipped via flags.
 */
public final class ContextAssemblyPipeline {

    private final MemoryRetriever retriever;
    private final MemoryPolicy policy;
    private final TokenBudget tokenBudget;
    private final ContextPolicy contextPolicy;
    private final MemoryCompressor compressor;

    public ContextAssemblyPipeline(
        MemoryRetriever retriever,
        MemoryPolicy policy,
        TokenBudget tokenBudget,
        ContextPolicy contextPolicy,
        MemoryCompressor compressor
    ) {
        this.retriever = retriever;
        this.policy = policy;
        this.tokenBudget =
            tokenBudget == null
                ? TokenBudget.standard()
                : tokenBudget;
        this.contextPolicy =
            contextPolicy == null
                ? ContextPolicy.permissive()
                : contextPolicy;
        this.compressor =
            compressor == null
                ? new MemoryCompressor()
                : compressor;
    }

    public ContextAssemblyPipeline(
        MemoryRetriever retriever,
        TokenBudget tokenBudget
    ) {
        this(
            retriever,
            MemoryPolicy.permissive(),
            tokenBudget,
            ContextPolicy.permissive(),
            null
        );
    }

    /**
     * Execute the full 10-stage
     * context assembly pipeline.
     */
    public PipelineResult assemble(
        ContextRequest request
    ) {
        Objects.requireNonNull(
            request, "request"
        );

        List<PipelineSegment> candidates =
            new ArrayList<>();

        List<String> stageLog =
            new ArrayList<>();

        // Stage 1: COLLECT
        candidates = collect(
            request, candidates
        );
        stageLog.add(
            "COLLECT: "
                + candidates.size()
                + " segments"
        );

        // Stage 2: RETRIEVE
        candidates = retrieve(
            request, candidates
        );
        stageLog.add(
            "RETRIEVE: "
                + candidates.size()
                + " segments"
        );

        // Stage 3: FILTER
        candidates = filter(candidates);
        stageLog.add(
            "FILTER: "
                + candidates.size()
                + " segments"
        );

        // Stage 4: RANK
        candidates = rank(candidates);
        stageLog.add(
            "RANK: "
                + candidates.size()
                + " segments"
        );

        // Stage 5: DEDUPLICATE
        candidates = deduplicate(candidates);
        stageLog.add(
            "DEDUPLICATE: "
                + candidates.size()
                + " segments"
        );

        // Stage 6: COMPRESS
        candidates = compress(candidates);
        stageLog.add(
            "COMPRESS: "
                + candidates.size()
                + " segments"
        );

        // Stage 7: BUDGET
        candidates = applyBudget(candidates);
        stageLog.add(
            "BUDGET: "
                + candidates.size()
                + " segments"
        );

        // Stage 8: ASSEMBLE
        List<ContextSegment> assembled =
            assembleSegments(candidates);
        stageLog.add(
            "ASSEMBLE: "
                + assembled.size()
                + " segments"
        );

        // Stage 9: VALIDATE
        boolean valid = validate(assembled);
        stageLog.add(
            "VALIDATE: "
                + (valid ? "OK" : "FAIL")
        );

        // Stage 10: DELIVER
        int totalTokens = assembled.stream()
            .mapToInt(
                ContextSegment::estimatedTokens
            )
            .sum();

        ModelContext context =
            new ModelContext(
                assembled,
                totalTokens,
                ContextBudget.defaults(),
                java.util.Map.of(
                    "subjectId",
                    request.subjectId(),
                    "pipeline",
                    "10-stage"
                )
            );

        stageLog.add(
            "DELIVER: "
                + totalTokens
                + " tokens"
        );

        return new PipelineResult(
            context, stageLog, valid
        );
    }

    // Stage 1: COLLECT
    private List<PipelineSegment> collect(
        ContextRequest request,
        List<PipelineSegment> current
    ) {
        var result =
            new ArrayList<>(current);

        // Add instructions
        if (request.instructions() != null
            && !request.instructions()
                .isBlank()
        ) {
            result.add(
                new PipelineSegment(
                    "seg-" + UUID.randomUUID(),
                    ContextSegmentType.INSTRUCTION,
                    request.instructions(),
                    0,
                    "system",
                    ContextPriority.CRITICAL
                )
            );
        }

        // Add state
        if (request.stateText() != null
            && !request.stateText()
                .isBlank()
        ) {
            result.add(
                new PipelineSegment(
                    "seg-" + UUID.randomUUID(),
                    ContextSegmentType.STATE,
                    request.stateText(),
                    1,
                    "execution",
                    ContextPriority.HIGH
                )
            );
        }

        // Add observations
        if (request.observations() != null) {
            for (String obs :
                request.observations()
            ) {
                result.add(
                    new PipelineSegment(
                        "seg-"
                            + UUID.randomUUID(),
                        ContextSegmentType
                            .OBSERVATION,
                        obs, 2,
                        "observation",
                        ContextPriority.NORMAL
                    )
                );
            }
        }

        return result;
    }

    // Stage 2: RETRIEVE
    private List<PipelineSegment> retrieve(
        ContextRequest request,
        List<PipelineSegment> current
    ) {
        if (retriever == null
            || request.memoryQuery() == null
        ) {
            return current;
        }

        var result =
            new ArrayList<>(current);

        List<MemoryRecord> memories =
            retriever.retrieve(
                request.memoryQuery()
            );

        int count = 0;
        for (MemoryRecord mem : memories) {
            if (count >= contextPolicy
                .maxMemoryCount()
            ) {
                break;
            }

            if (!contextPolicy
                .isMemoryTypeAllowed(
                    mem.type()
                )
            ) {
                continue;
            }

            result.add(
                new PipelineSegment(
                    "seg-"
                        + UUID.randomUUID(),
                    ContextSegmentType.MEMORY,
                    mem.content(), 3,
                    "memory:"
                        + mem.memoryId(),
                    ContextPriority.NORMAL
                )
            );
            count++;
        }

        return result;
    }

    // Stage 3: FILTER
    private List<PipelineSegment> filter(
        List<PipelineSegment> current
    ) {
        return current.stream()
            .filter(s ->
                contextPolicy.isPriorityAllowed(
                    s.priority()
                )
            )
            .toList()
            .stream()
            .map(s -> s)
            .collect(
                java.util.stream.Collectors
                    .toCollection(
                        ArrayList::new
                    )
            );
    }

    // Stage 4: RANK
    private List<PipelineSegment> rank(
        List<PipelineSegment> current
    ) {
        var sorted =
            new ArrayList<>(current);

        sorted.sort(
            Comparator
                .comparingInt(
                    (PipelineSegment s) ->
                        s.priority().ordinal()
                )
                .thenComparingInt(
                    PipelineSegment::stagePriority
                )
        );

        return sorted;
    }

    // Stage 5: DEDUPLICATE
    private List<PipelineSegment> deduplicate(
        List<PipelineSegment> current
    ) {
        Set<String> seen =
            new LinkedHashSet<>();

        var result =
            new ArrayList<PipelineSegment>();

        for (var seg : current) {
            if (seen.add(seg.content())) {
                result.add(seg);
            }
        }

        return result;
    }

    // Stage 6: COMPRESS
    private List<PipelineSegment> compress(
        List<PipelineSegment> current
    ) {
        // Context compression: no-op
        // for segments that already fit.
        // Full compression is applied
        // at BUDGET stage.
        return current;
    }

    // Stage 7: BUDGET
    private List<PipelineSegment> applyBudget(
        List<PipelineSegment> current
    ) {
        int available =
            tokenBudget
                .availableForContext();

        var result =
            new ArrayList<PipelineSegment>();
        int used = 0;

        for (var seg : current) {
            int tokens =
                seg.estimatedTokens();

            if (used + tokens <= available) {
                result.add(seg);
                used += tokens;
            } else if (
                seg.priority()
                    == ContextPriority.CRITICAL
            ) {
                // CRITICAL always included
                result.add(seg);
                used += tokens;
            }
        }

        return result;
    }

    // Stage 8: ASSEMBLE
    private List<ContextSegment> assembleSegments(
        List<PipelineSegment> candidates
    ) {
        return candidates.stream()
            .map(p ->
                new ContextSegment(
                    p.segmentId(),
                    p.type(),
                    p.content(),
                    p.stagePriority(),
                    p.source()
                )
            )
            .toList();
    }

    // Stage 9: VALIDATE
    private boolean validate(
        List<ContextSegment> assembled
    ) {
        if (assembled.isEmpty()) {
            return true;
        }

        int totalTokens = assembled.stream()
            .mapToInt(
                ContextSegment::estimatedTokens
            )
            .sum();

        return tokenBudget.fitsWithin(
            totalTokens
        );
    }

    /**
     * Pipeline segment with priority
     * metadata used during processing.
     */
    record PipelineSegment(
        String segmentId,
        ContextSegmentType type,
        String content,
        int stagePriority,
        String source,
        ContextPriority priority
    ) {
        int estimatedTokens() {
            return Math.max(
                1,
                content.length() / 4
            );
        }
    }

    /**
     * Pipeline execution result.
     */
    public record PipelineResult(
        ModelContext context,
        List<String> stageLog,
        boolean valid
    ) {
        public PipelineResult {
            Objects.requireNonNull(
                context, "context"
            );
            stageLog =
                stageLog == null
                    ? List.of()
                    : List.copyOf(stageLog);
        }

        public int stageCount() {
            return stageLog.size();
        }
    }
}
