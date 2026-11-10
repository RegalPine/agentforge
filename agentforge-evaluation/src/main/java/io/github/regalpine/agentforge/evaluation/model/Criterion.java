package io.github.regalpine.agentforge.evaluation.model;

import java.util.Objects;

/**
 * Criterion — AE-12 §13-14.
 *
 * A single evaluation criterion with id, name, type, weight and threshold.
 */
public final class Criterion {

    private final String id;
    private final String name;
    private final String description;
    private final CriterionType type;
    private final double weight;
    private final double threshold;

    public Criterion(String id, String name, String description, CriterionType type, double weight, double threshold) {
        this.id = Objects.requireNonNull(id, "id");
        this.name = Objects.requireNonNull(name, "name");
        this.description = Objects.requireNonNull(description, "description");
        this.type = Objects.requireNonNull(type, "type");
        if (weight < 0.0 || weight > 1.0) {
            throw new IllegalArgumentException("weight must be in [0, 1]: " + weight);
        }
        if (threshold < 0.0 || threshold > 1.0) {
            throw new IllegalArgumentException("threshold must be in [0, 1]: " + threshold);
        }
        this.weight = weight;
        this.threshold = threshold;
    }

    public static Criterion of(String id, CriterionType type, double threshold) {
        return new Criterion(id, id, "", type, 1.0, threshold);
    }

    public String id() {
        return id;
    }

    public String name() {
        return name;
    }

    public String description() {
        return description;
    }

    public CriterionType type() {
        return type;
    }

    public double weight() {
        return weight;
    }

    public double threshold() {
        return threshold;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Criterion that)) return false;
        return id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public String toString() {
        return "Criterion{id='" + id + "', type=" + type + ", weight=" + weight + ", threshold=" + threshold + "}";
    }
}
