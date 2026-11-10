package io.github.regalpine.agentforge.evolution;

import java.util.List;

/**
 * Candidate generator SPI.
 * AF-ENHANCE-15 §29: Candidate
 * Generator.
 */
public interface CandidateGenerator {

    /**
     * Generate candidates from a
     * problem.
     */
    List<EvolutionCandidate> generate(
        EvolutionProblem problem,
        EvolutionContext context
    );
}
