package io.github.regalpine.agentforge.hardening;

import java.util.Objects;

/**
 * TCK test level.
 * AF-RELEASE-02 §4: six levels from
 * L0 (unit) to L5 (production gate).
 */
public enum TestLevel {

    /**
     * L0 — Unit test.
     */
    L0_UNIT(0, "Unit"),

    /**
     * L1 — Integration test.
     */
    L1_INTEGRATION(1, "Integration"),

    /**
     * L2 — Module conformance.
     */
    L2_CONFORMANCE(2, "Conformance"),

    /**
     * L3 — Core capability E2E.
     */
    L3_E2E_CORE(3, "E2E Core"),

    /**
     * L4 — Distributed / Enterprise.
     */
    L4_DISTRIBUTED(4, "Distributed"),

    /**
     * L5 — Production release gate.
     */
    L5_RELEASE(5, "Release Gate");

    private final int level;
    private final String label;

    TestLevel(int level, String label) {
        this.level = level;
        this.label = label;
    }

    /**
     * Numeric level (0-5).
     */
    public int level() {
        return level;
    }

    /**
     * Human-readable label.
     */
    public String label() {
        return label;
    }

    /**
     * Whether this level is at or
     * above the given level.
     */
    public boolean isAtLeast(TestLevel other) {
        return this.level >= other.level;
    }
}
