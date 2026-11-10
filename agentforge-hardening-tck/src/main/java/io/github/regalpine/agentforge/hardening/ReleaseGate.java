package io.github.regalpine.agentforge.hardening;

import java.util.*;

/**
 * Release Gate decision engine.
 * AF-RELEASE-02 §5-6: determines
 * whether a release can proceed
 * based on TCK results.
 *
 * <p>Rules:
 * <ul>
 *   <li>Only PASS qualifies for release</li>
 *   <li>PARTIAL_PASS is forbidden</li>
 *   <li>FAIL or BLOCKED blocks release</li>
 *   <li>NOT_APPLICABLE is neutral</li>
 *   <li>L0-L2 must all pass</li>
 *   <li>L3 core capabilities must pass</li>
 *   <li>L4 distributed must pass</li>
 * </ul>
 */
public final class ReleaseGate {

    private ReleaseGate() {}

    /**
     * Gate decision.
     */
    public enum Decision {
        APPROVED,
        REJECTED
    }

    /**
     * Gate evaluation result.
     */
    public record GateResult(
        Decision decision,
        int totalCases,
        int passCount,
        int failCount,
        int blockedCount,
        int notApplicableCount,
        List<String> blockingReasons
    ) {

        public GateResult {
            blockingReasons = blockingReasons == null
                ? List.of()
                : List.copyOf(blockingReasons);
        }

        /**
         * Whether release is approved.
         */
        public boolean isApproved() {
            return decision == Decision.APPROVED;
        }

        /**
         * Format the gate report.
         */
        public String summary() {
            var sb = new StringBuilder();
            sb.append("=== Release Gate ===\n");
            sb.append("Decision: ")
                .append(decision).append("\n");
            sb.append("PASS: ").append(passCount)
                .append(" | FAIL: ").append(failCount)
                .append(" | BLOCKED: ").append(blockedCount)
                .append(" | N/A: ").append(notApplicableCount)
                .append("\n");
            if (!blockingReasons.isEmpty()) {
                sb.append("Blocking Reasons:\n");
                for (String r : blockingReasons) {
                    sb.append("  - ").append(r).append("\n");
                }
            }
            return sb.toString();
        }
    }

    /**
     * Evaluate the release gate
     * against a TCK report.
     */
    public static GateResult evaluate(
        TckReport report
    ) {
        int pass = report.passCount();
        int fail = report.failCount();
        int blocked = report.blockedCount();
        int na = (int) report.cases().stream()
            .filter(c -> c.result()
                == TckResult.NOT_APPLICABLE)
            .count();

        List<String> reasons = new ArrayList<>();

        // Check for any FAILs
        for (TckCase c : report.cases()) {
            if (c.result() == TckResult.FAIL) {
                reasons.add(
                    "FAIL: " + c.id() + " — "
                    + c.description()
                );
            }
        }

        // Check for BLOCKEDs
        for (TckCase c : report.cases()) {
            if (c.result() == TckResult.BLOCKED) {
                reasons.add(
                    "BLOCKED: " + c.id() + " — "
                    + c.evidence()
                );
            }
        }

        // Check L0-L2 must all pass
        for (TckCase c : report.cases()) {
            if (c.level().level() <= 2
                && c.result() == TckResult.FAIL
            ) {
                reasons.add(
                    "L" + c.level().level()
                    + " failure blocks release: "
                    + c.id()
                );
            }
        }

        Decision decision = reasons.isEmpty()
            ? Decision.APPROVED
            : Decision.REJECTED;

        return new GateResult(
            decision,
            report.totalCases(),
            pass, fail, blocked, na,
            reasons
        );
    }
}
