package io.github.regalpine.agentforge.capability;

/**
 * Evaluation evidence SPI for the
 * capability release gate.
 * AF-SKILL-03 §30: EvaluationGate
 *   Integration.
 *
 * <p>Defines the minimal contract
 * that the capability module needs
 * from evaluation evidence. The
 * evaluation module's concrete
 * evidence type implements this
 * interface.
 *
 * <p>This SPI decouples capability
 * from the evaluation module —
 * capability depends only on this
 * interface, not on evaluation
 * types directly.
 */
public interface EvaluationEvidence {

    /**
     * Check if the evaluation passed.
     */
    boolean isPassed();

    /**
     * The Skill ID this evidence
     * pertains to.
     */
    String skillId();

    /**
     * The Skill version this evidence
     * pertains to.
     */
    String skillVersion();
}
