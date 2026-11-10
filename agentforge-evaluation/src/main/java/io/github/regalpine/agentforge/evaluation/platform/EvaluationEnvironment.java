package io.github.regalpine.agentforge.evaluation.platform;

import java.util.Map;

/**
 * Evaluation environment.
 * AF-ENHANCE-12 §73: Environment.
 */
public record EvaluationEnvironment(
    String runtimeVersion,
    String modelVersion,
    Map<String, String> configuration,
    String environmentDigest
) {

    public EvaluationEnvironment {
        configuration = configuration == null
            ? Map.of() : Map.copyOf(configuration);
        environmentDigest = environmentDigest == null
            ? "" : environmentDigest;
    }

    /**
     * Create a default environment.
     */
    public static EvaluationEnvironment defaults() {
        return new EvaluationEnvironment(
            "1.0.0", "", Map.of(), ""
        );
    }
}
