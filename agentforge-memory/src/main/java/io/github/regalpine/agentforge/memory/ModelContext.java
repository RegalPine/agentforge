package io.github.regalpine.agentforge.memory;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Assembled model context.
 * AE-08 §40-42: Context Builder output.
 *
 * <p>The result of context assembly.
 * Contains prioritized segments that
 * fit within the budget, along with
 * provenance and token usage metadata.
 */
public record ModelContext(
    List<ContextSegment> segments,
    int totalTokens,
    ContextBudget budget,
    Map<String, Object> metadata
) {

    public ModelContext {
        Objects.requireNonNull(
            segments, "segments"
        );
        Objects.requireNonNull(
            budget, "budget"
        );

        segments = List.copyOf(segments);

        metadata =
            metadata == null
                ? Map.of()
                : Map.copyOf(metadata);

        totalTokens = segments.stream()
            .mapToInt(ContextSegment::estimatedTokens)
            .sum();
    }

    /**
     * Get the fingerprint for this context.
     * AE-08 §65: Context Fingerprint.
     */
    public String fingerprint() {
        StringBuilder sb = new StringBuilder();
        for (ContextSegment seg : segments) {
            sb.append(seg.segmentId())
              .append(':')
              .append(seg.type())
              .append(',');
        }
        return sb.toString();
    }

    /**
     * Get segments of a specific type.
     */
    public List<ContextSegment> segmentsOfType(
        ContextSegmentType type
    ) {
        return segments.stream()
            .filter(s -> s.type() == type)
            .toList();
    }

    /**
     * Get the full assembled text.
     */
    public String assembledText() {
        StringBuilder sb = new StringBuilder();
        for (ContextSegment seg : segments) {
            if (!sb.isEmpty()) {
                sb.append("\n\n");
            }
            sb.append(seg.content());
        }
        return sb.toString();
    }
}
