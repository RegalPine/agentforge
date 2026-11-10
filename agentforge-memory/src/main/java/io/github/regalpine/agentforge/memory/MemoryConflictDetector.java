package io.github.regalpine.agentforge.memory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Memory conflict detector.
 * AF-ENHANCE-06 §16, §54: Memory Conflict.
 *
 * <p>Detects when multiple memories
 * carry contradictory information
 * for the same subject. Conflict
 * resolution follows:
 *
 * <pre>
 * Conflict Detection
 *   → Provenance Comparison
 *   → Validity Comparison
 *   → Policy
 *   → Resolution Strategy
 * </pre>
 *
 * <p>Runtime does NOT simply
 * overwrite (no "last write wins").
 */
public final class MemoryConflictDetector {

    /**
     * Detect conflicts among a set
     * of memory records. Two records
     * conflict if they share the same
     * subject + type but have
     * different content.
     */
    public List<ConflictGroup> detect(
        List<MemoryRecord> records
    ) {
        Objects.requireNonNull(
            records, "records"
        );

        Map<String, List<MemoryRecord>>
            groups = new HashMap<>();

        for (MemoryRecord rec : records) {
            String key =
                rec.subjectId()
                    + "|" + rec.type().name();

            groups.computeIfAbsent(
                key, k -> new ArrayList<>()
            ).add(rec);
        }

        List<ConflictGroup> conflicts =
            new ArrayList<>();

        for (var entry : groups.entrySet()) {
            List<MemoryRecord> group =
                entry.getValue();

            if (group.size() < 2) {
                continue;
            }

            // Check if content differs
            boolean hasConflict = false;
            String firstContent =
                group.get(0).content();

            for (int i = 1;
                 i < group.size(); i++
            ) {
                if (!firstContent.equals(
                    group.get(i).content()
                )) {
                    hasConflict = true;
                    break;
                }
            }

            if (hasConflict) {
                conflicts.add(
                    new ConflictGroup(
                        entry.getKey(),
                        List.copyOf(group)
                    )
                );
            }
        }

        return conflicts;
    }

    /**
     * Resolve a conflict group
     * using the given strategy.
     */
    public MemoryRecord resolve(
        ConflictGroup group,
        ResolutionStrategy strategy
    ) {
        Objects.requireNonNull(
            group, "group"
        );
        Objects.requireNonNull(
            strategy, "strategy"
        );

        return switch (strategy) {
            case NEWEST_WINS ->
                resolveNewest(group);
            case HIGHEST_CONFIDENCE ->
                resolveByConfidence(group);
            case HIGHEST_IMPORTANCE ->
                resolveByImportance(group);
        };
    }

    private MemoryRecord resolveNewest(
        ConflictGroup group
    ) {
        MemoryRecord newest =
            group.records().get(0);

        for (MemoryRecord rec :
            group.records()
        ) {
            if (rec.createdAt().isAfter(
                newest.createdAt()
            )) {
                newest = rec;
            }
        }

        return newest;
    }

    private MemoryRecord resolveByConfidence(
        ConflictGroup group
    ) {
        MemoryRecord best =
            group.records().get(0);

        for (MemoryRecord rec :
            group.records()
        ) {
            if (rec.confidence().score()
                > best.confidence().score()
            ) {
                best = rec;
            }
        }

        return best;
    }

    private MemoryRecord resolveByImportance(
        ConflictGroup group
    ) {
        MemoryRecord best =
            group.records().get(0);

        for (MemoryRecord rec :
            group.records()
        ) {
            if (rec.importance().ordinal()
                > best.importance().ordinal()
            ) {
                best = rec;
            }
        }

        return best;
    }

    /**
     * A group of conflicting
     * memory records.
     */
    public record ConflictGroup(
        String conflictKey,
        List<MemoryRecord> records
    ) {
        public ConflictGroup {
            Objects.requireNonNull(
                conflictKey, "conflictKey"
            );
            records = List.copyOf(records);
        }

        public int size() {
            return records.size();
        }
    }

    /**
     * Conflict resolution strategy.
     */
    public enum ResolutionStrategy {
        NEWEST_WINS,
        HIGHEST_CONFIDENCE,
        HIGHEST_IMPORTANCE
    }
}
