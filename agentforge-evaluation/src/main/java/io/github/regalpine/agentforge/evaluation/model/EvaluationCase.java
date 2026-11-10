package io.github.regalpine.agentforge.evaluation.model;

import java.util.Objects;

/**
 * EvaluationCase — AE-12 §5.
 *
 * The fundamental unit of evaluation: input + expected outcome + criteria.
 */
public final class EvaluationCase {

    private final String id;
    private final String version;
    private final EvaluationInput input;
    private final ExpectedOutcome expected;
    private final EvaluationCriteria criteria;

    public EvaluationCase(
            String id,
            String version,
            EvaluationInput input,
            ExpectedOutcome expected,
            EvaluationCriteria criteria) {
        this.id = Objects.requireNonNull(id, "id");
        this.version = Objects.requireNonNull(version, "version");
        this.input = Objects.requireNonNull(input, "input");
        this.expected = Objects.requireNonNull(expected, "expected");
        this.criteria = Objects.requireNonNull(criteria, "criteria");
    }

    public String id() {
        return id;
    }

    public String version() {
        return version;
    }

    public EvaluationInput input() {
        return input;
    }

    public ExpectedOutcome expected() {
        return expected;
    }

    public EvaluationCriteria criteria() {
        return criteria;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof EvaluationCase that)) return false;
        return id.equals(that.id) && version.equals(that.version);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, version);
    }

    @Override
    public String toString() {
        return "EvaluationCase{id='" + id + "', version='" + version + "'}";
    }
}
