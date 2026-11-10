package io.github.regalpine.agentforge.capability;

import io.github.regalpine.agentforge.skill.governance.SkillDigest;
import io.github.regalpine.agentforge.skill.governance.SkillGovernanceProfile;
import io.github.regalpine.agentforge.skill.governance.SkillLifecycleState;
import io.github.regalpine.agentforge.skill.governance.SkillPackageMetadata;
import io.github.regalpine.agentforge.kernel.SkillReference;
import io.github.regalpine.agentforge.skill.governance.SkillSecurityProfile;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Skill 发布门禁.
 * AF-SKILL-03 §23-§26: Release Gate.
 * AF-AD-15 §8.5: 发布门禁流水线.
 *
 * <p>Pipeline:
 * <pre>
 * Package → Integrity Verification
 * → Dependency Validation → Security Scan
 * → Test → Evaluation → Governance Approval
 * → Publish → Activate
 * </pre>
 *
 * <p>N: Publish ≠ Activate.
 * Any critical gate failure → NOT PUBLISHED.
 */
public final class SkillReleaseGate {

    private final List<GateCheck> checks;

    private SkillReleaseGate(
        List<GateCheck> checks
    ) {
        this.checks = List.copyOf(checks);
    }

    /**
     * Run all gate checks.
     *
     * @return the gate result
     */
    public GateResult evaluate(
        SkillPackageMetadata metadata,
        SkillDigest digest,
        SkillSecurityProfile security,
        SkillGovernanceProfile governance
    ) {
        Map<String, GateStatus> results =
            new LinkedHashMap<>();

        for (GateCheck check : checks) {
            GateStatus status = check.evaluate(
                metadata, digest,
                security, governance
            );

            results.put(
                check.name(),
                status
            );

            // Critical failure → stop
            if (status == GateStatus.FAIL) {
                return new GateResult(
                    GateDecision.NOT_PUBLISHED,
                    results,
                    Instant.now()
                );
            }
        }

        // All passed → PUBLISHED
        // (not yet ACTIVATED)
        return new GateResult(
            GateDecision.PUBLISHED,
            results,
            Instant.now()
        );
    }

    /**
     * Activate a published Skill.
     * N: Publish ≠ Activate.
     */
    public boolean activate(
        SkillRegistry registry,
        SkillReference reference
    ) {
        SkillLifecycleState state =
            registry.state(reference);

        if (state !=
            SkillLifecycleState.PUBLISHED
        ) {
            return false;
        }

        registry.transition(
            reference,
            SkillLifecycleState.ACTIVE
        );

        return true;
    }

    public static Builder builder() {
        return new Builder();
    }

    /**
     * Gate check SPI.
     */
    public interface GateCheck {
        String name();

        GateStatus evaluate(
            SkillPackageMetadata metadata,
            SkillDigest digest,
            SkillSecurityProfile security,
            SkillGovernanceProfile governance
        );
    }

    /**
     * Gate check result status.
     */
    public enum GateStatus {
        PASS,
        FAIL,
        WARN,
        SKIPPED
    }

    /**
     * Overall gate decision.
     */
    public enum GateDecision {
        PUBLISHED,
        NOT_PUBLISHED
    }

    /**
     * Gate evaluation result.
     */
    public record GateResult(
        GateDecision decision,
        Map<String, GateStatus> checkResults,
        Instant evaluatedAt
    ) {
        public GateResult {
            checkResults = Map.copyOf(checkResults);
        }

        public boolean isPublished() {
            return decision ==
                GateDecision.PUBLISHED;
        }
    }

    public static final class Builder {
        private final List<GateCheck> checks =
            new ArrayList<>();

        public Builder addCheck(GateCheck check) {
            checks.add(
                Objects.requireNonNull(check)
            );
            return this;
        }

        public SkillReleaseGate build() {
            return new SkillReleaseGate(checks);
        }
    }
}
