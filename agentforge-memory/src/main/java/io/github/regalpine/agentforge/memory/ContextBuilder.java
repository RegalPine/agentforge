package io.github.regalpine.agentforge.memory;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * Context builder.
 * AE-08 §40-45: Context Builder.
 *
 * <p>Assembles a ModelContext from
 * multiple sources: instructions, state,
 * observations, memory, capabilities.
 * Applies budget constraints and
 * priority ordering.
 *
 * <p>Context = Instructions + State +
 * Observations + Memory + Capabilities +
 * Policy
 */
public final class ContextBuilder {

    private final MemoryRetriever retriever;
    private final ContextBudget budget;
    private final MemoryEventSink eventSink;

    public ContextBuilder(
        MemoryRetriever retriever,
        ContextBudget budget,
        MemoryEventSink eventSink
    ) {
        this.retriever = retriever;
        this.budget =
            budget == null
                ? ContextBudget.defaults()
                : budget;
        this.eventSink = eventSink;
    }

    public ContextBuilder(
        MemoryRetriever retriever,
        ContextBudget budget
    ) {
        this(retriever, budget, null);
    }

    public ContextBuilder(
        MemoryRetriever retriever
    ) {
        this(retriever, ContextBudget.defaults(), null);
    }

    /**
     * Build a model context from the
     * given context request.
     */
    public ModelContext build(
        ContextRequest request
    ) {
        Objects.requireNonNull(
            request, "request"
        );

        emitEvent(
            MemoryEventType.CONTEXT_BUILD_STARTED,
            request.subjectId()
        );

        List<ContextSegment> allSegments =
            new ArrayList<>();

        // 1. Instructions (highest priority)
        if (request.instructions() != null
            && !request.instructions().isBlank()
        ) {
            allSegments.add(
                new ContextSegment(
                    "seg-" + UUID.randomUUID(),
                    ContextSegmentType.INSTRUCTION,
                    request.instructions(),
                    0,
                    "system"
                )
            );
        }

        // 2. State
        if (request.stateText() != null
            && !request.stateText().isBlank()
        ) {
            allSegments.add(
                new ContextSegment(
                    "seg-" + UUID.randomUUID(),
                    ContextSegmentType.STATE,
                    request.stateText(),
                    1,
                    "execution"
                )
            );
        }

        // 3. Observations
        if (request.observations() != null) {
            for (String obs : request.observations()) {
                allSegments.add(
                    new ContextSegment(
                        "seg-" + UUID.randomUUID(),
                        ContextSegmentType.OBSERVATION,
                        obs,
                        2,
                        "observation"
                    )
                );
            }
        }

        // 4. Memory (via retrieval)
        if (retriever != null
            && request.memoryQuery() != null
        ) {
            List<MemoryRecord> memories =
                retriever.retrieve(
                    request.memoryQuery()
                );

            int memBudget = budget.memoryTokens();
            int memUsed = 0;

            for (MemoryRecord mem : memories) {
                int tokens =
                    mem.content().length() / 4;

                if (memUsed + tokens > memBudget) {
                    break;
                }

                allSegments.add(
                    new ContextSegment(
                        "seg-" + UUID.randomUUID(),
                        ContextSegmentType.MEMORY,
                        mem.content(),
                        3,
                        "memory:" + mem.memoryId()
                    )
                );

                memUsed += tokens;
            }
        }

        // 5. Sort by priority and fit budget
        allSegments.sort(
            Comparator.comparingInt(
                ContextSegment::priority
            )
        );

        List<ContextSegment> fitted =
            fitToBudget(allSegments);

        ModelContext context = new ModelContext(
            fitted, 0, budget,
            Map.of(
                "subjectId",
                request.subjectId()
            )
        );

        emitEvent(
            MemoryEventType.CONTEXT_BUILD_COMPLETED,
            request.subjectId()
        );

        return context;
    }

    private List<ContextSegment> fitToBudget(
        List<ContextSegment> segments
    ) {
        List<ContextSegment> result =
            new ArrayList<>();

        int used = 0;

        for (ContextSegment seg : segments) {
            int tokens = seg.estimatedTokens();

            if (used + tokens <= budget.maxTokens()) {
                result.add(seg);
                used += tokens;
            }
        }

        return result;
    }

    private void emitEvent(
        MemoryEventType type,
        String subjectId
    ) {
        if (eventSink != null) {
            eventSink.emit(
                new MemoryEvent(
                    type,
                    subjectId == null
                        ? "" : subjectId,
                    System.currentTimeMillis()
                )
            );
        }
    }
}
