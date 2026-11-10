package io.github.regalpine.agentforge.evaluation.model;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * EvaluationInput — AE-12 §6.
 *
 * Carries the task, context and parameters for a single evaluation case.
 * Must be versionable.
 */
public final class EvaluationInput {

    private final Map<String, Object> task;
    private final Map<String, Object> context;
    private final Map<String, Object> parameters;

    public EvaluationInput(
            Map<String, Object> task,
            Map<String, Object> context,
            Map<String, Object> parameters) {
        this.task = Objects.requireNonNull(task, "task");
        this.context = Objects.requireNonNull(context, "context");
        this.parameters = Objects.requireNonNull(parameters, "parameters");
    }

    public Map<String, Object> task() {
        return task;
    }

    public Map<String, Object> context() {
        return context;
    }

    public Map<String, Object> parameters() {
        return parameters;
    }

    public Optional<Object> getParameter(String key) {
        return Optional.ofNullable(parameters.get(key));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof EvaluationInput that)) return false;
        return task.equals(that.task) && context.equals(that.context) && parameters.equals(that.parameters);
    }

    @Override
    public int hashCode() {
        return Objects.hash(task, context, parameters);
    }

    @Override
    public String toString() {
        return "EvaluationInput{task=" + task + ", context=" + context + ", parameters=" + parameters + "}";
    }
}
