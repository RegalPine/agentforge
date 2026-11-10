package io.github.regalpine.agentforge.evolution;

/**
 * Validation gate SPI.
 * AF-ENHANCE-15 §34: Validation
 * Pipeline.
 *
 * <p>Five gates: Test, Evaluation,
 * Regression, Policy, Security.
 */
public interface ValidationGate {

    /**
     * Gate name.
     */
    String name();

    /**
     * Validate a candidate.
     */
    ValidationResult validate(
        EvolutionCandidate candidate
    );
}
