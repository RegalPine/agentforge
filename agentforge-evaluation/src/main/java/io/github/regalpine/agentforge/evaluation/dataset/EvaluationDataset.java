package io.github.regalpine.agentforge.evaluation.dataset;

import io.github.regalpine.agentforge.evaluation.model.EvaluationCase;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * EvaluationDataset — AE-12 §36.
 *
 * A versioned collection of evaluation cases.
 */
public final class EvaluationDataset {

    private final String id;
    private final String version;
    private final List<EvaluationCase> cases;
    private final DatasetType type;

    public EvaluationDataset(String id, String version, List<EvaluationCase> cases, DatasetType type) {
        this.id = Objects.requireNonNull(id, "id");
        this.version = Objects.requireNonNull(version, "version");
        this.cases = List.copyOf(Objects.requireNonNull(cases, "cases"));
        this.type = Objects.requireNonNull(type, "type");
    }

    public static EvaluationDataset of(String id, String version, List<EvaluationCase> cases) {
        return new EvaluationDataset(id, version, cases, DatasetType.UNIT);
    }

    public String id() {
        return id;
    }

    public String version() {
        return version;
    }

    public List<EvaluationCase> cases() {
        return cases;
    }

    public DatasetType type() {
        return type;
    }

    public int size() {
        return cases.size();
    }

    public Optional<EvaluationCase> findCase(String caseId) {
        return cases.stream().filter(c -> c.id().equals(caseId)).findFirst();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof EvaluationDataset that)) return false;
        return id.equals(that.id) && version.equals(that.version);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, version);
    }

    @Override
    public String toString() {
        return "EvaluationDataset{id='" + id + "', version='" + version + "', type=" + type + ", size=" + cases.size() + "}";
    }
}
