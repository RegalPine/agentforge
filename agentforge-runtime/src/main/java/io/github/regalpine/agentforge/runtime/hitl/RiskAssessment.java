package io.github.regalpine.agentforge.runtime.hitl;

import io.github.regalpine.agentforge.event.Execution;
import java.util.Map;
import java.util.Objects;

/**
 * 风险评估。
 *
 * <p>对当前 Execution Action 的风险判断。
 * 对应 AF-ENHANCE-03 §6。</p>
 */
public record RiskAssessment(
    RiskLevel level,
    String category,
    double score,
    String reason,
    String policyId,
    Map<String, Object> evidence
) {

    public RiskAssessment {
        Objects.requireNonNull(
            level, "level"
        );
        if (score < 0.0
            || score > 1.0
        ) {
            throw new IllegalArgumentException(
                "score must be in [0.0, 1.0]"
            );
        }
        evidence = evidence == null
            ? Map.of()
            : Map.copyOf(evidence);
    }

    public static RiskAssessment of(
        RiskLevel level,
        String reason
    ) {
        return new RiskAssessment(
            level, null, 0.0,
            reason, null, Map.of()
        );
    }

    public static RiskAssessment of(
        RiskLevel level,
        String category,
        double score,
        String reason
    ) {
        return new RiskAssessment(
            level, category, score,
            reason, null, Map.of()
        );
    }
}
