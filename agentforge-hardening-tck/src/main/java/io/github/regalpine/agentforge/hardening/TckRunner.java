package io.github.regalpine.agentforge.hardening;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * TCK runner.
 * AF-RELEASE-02 §8: collects conformance
 * test results and produces a report.
 */
public final class TckRunner {

    private final String reportId;
    private final List<TckCase> cases =
        new CopyOnWriteArrayList<>();
    private final Map<String, String> metadata =
        new LinkedHashMap<>();

    /**
     * Create a runner.
     */
    public TckRunner(String reportId) {
        this.reportId =
            Objects.requireNonNull(reportId);
    }

    /**
     * Add metadata.
     */
    public TckRunner meta(String key, String val) {
        metadata.put(key, val);
        return this;
    }

    /**
     * Record a PASS result.
     */
    public TckRunner recordPass(
        TckId id, String domain,
        String desc, TestLevel level
    ) {
        cases.add(TckCase.pass(
            id, domain, desc, level
        ));
        return this;
    }

    /**
     * Record a FAIL result.
     */
    public TckRunner recordFail(
        TckId id, String domain,
        String desc, TestLevel level,
        String evidence
    ) {
        cases.add(TckCase.fail(
            id, domain, desc, level, evidence
        ));
        return this;
    }

    /**
     * Record a BLOCKED result.
     */
    public TckRunner recordBlocked(
        TckId id, String domain,
        String desc, TestLevel level,
        String reason
    ) {
        cases.add(TckCase.blocked(
            id, domain, desc, level, reason
        ));
        return this;
    }

    /**
     * Record a NOT_APPLICABLE result.
     */
    public TckRunner recordNA(
        TckId id, String domain,
        String desc, TestLevel level
    ) {
        cases.add(TckCase.notApplicable(
            id, domain, desc, level
        ));
        return this;
    }

    /**
     * Record a case directly.
     */
    public TckRunner record(TckCase c) {
        cases.add(
            Objects.requireNonNull(c)
        );
        return this;
    }

    /**
     * Build the report.
     */
    public TckReport buildReport() {
        return new TckReport(
            reportId, Instant.now(),
            List.copyOf(cases),
            Map.copyOf(metadata)
        );
    }

    /**
     * Evaluate the release gate.
     */
    public ReleaseGate.GateResult evaluateGate() {
        return ReleaseGate.evaluate(buildReport());
    }

    /**
     * Get the number of recorded cases.
     */
    public int caseCount() {
        return cases.size();
    }
}
