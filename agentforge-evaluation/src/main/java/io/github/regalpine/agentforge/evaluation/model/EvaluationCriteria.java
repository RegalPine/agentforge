package io.github.regalpine.agentforge.evaluation.model;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * EvaluationCriteria — AE-12 §13.
 *
 * A collection of Criterion items for an evaluation case.
 */
public final class EvaluationCriteria {

    private final List<Criterion> criteria;

    public EvaluationCriteria(List<Criterion> criteria) {
        this.criteria = List.copyOf(Objects.requireNonNull(criteria, "criteria"));
    }

    public static EvaluationCriteria of(Criterion... criteria) {
        return new EvaluationCriteria(List.of(criteria));
    }

    public List<Criterion> criteria() {
        return criteria;
    }

    public Optional<Criterion> find(String criterionId) {
        return criteria.stream().filter(c -> c.id().equals(criterionId)).findFirst();
    }

    public int size() {
        return criteria.size();
    }

    public boolean isEmpty() {
        return criteria.isEmpty();
    }

    @Override
    public String toString() {
        return "EvaluationCriteria{criteria=" + criteria + "}";
    }
}
