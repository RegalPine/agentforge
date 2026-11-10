package io.github.regalpine.agentforge.hardening;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;

/**
 * A single TCK test case record.
 * AF-RELEASE-02 §7: each case carries
 * its ID, domain, level, result, and
 * optional evidence.
 */
public record TckCase(
    TckId id,
    String domain,
    String description,
    TestLevel level,
    TckResult result,
    Instant timestamp,
    String evidence,
    Map<String, String> metadata
) {

    public TckCase {
        Objects.requireNonNull(id);
        Objects.requireNonNull(domain);
        Objects.requireNonNull(description);
        Objects.requireNonNull(level);
        Objects.requireNonNull(result);
        timestamp = timestamp == null
            ? Instant.now() : timestamp;
        evidence = evidence == null
            ? "" : evidence;
        metadata = metadata == null
            ? Map.of() : Map.copyOf(metadata);
    }

    /**
     * Create a PASS case.
     */
    public static TckCase pass(
        TckId id, String domain,
        String desc, TestLevel level
    ) {
        return new TckCase(
            id, domain, desc, level,
            TckResult.PASS, Instant.now(),
            "Assertion passed", Map.of()
        );
    }

    /**
     * Create a FAIL case.
     */
    public static TckCase fail(
        TckId id, String domain,
        String desc, TestLevel level,
        String evidence
    ) {
        return new TckCase(
            id, domain, desc, level,
            TckResult.FAIL, Instant.now(),
            evidence, Map.of()
        );
    }

    /**
     * Create a BLOCKED case.
     */
    public static TckCase blocked(
        TckId id, String domain,
        String desc, TestLevel level,
        String reason
    ) {
        return new TckCase(
            id, domain, desc, level,
            TckResult.BLOCKED, Instant.now(),
            reason, Map.of()
        );
    }

    /**
     * Create a NOT_APPLICABLE case.
     */
    public static TckCase notApplicable(
        TckId id, String domain,
        String desc, TestLevel level
    ) {
        return new TckCase(
            id, domain, desc, level,
            TckResult.NOT_APPLICABLE,
            Instant.now(),
            "Not applicable", Map.of()
        );
    }

    /**
     * Whether this case passed.
     */
    public boolean isPassing() {
        return result.isPassing();
    }
}
