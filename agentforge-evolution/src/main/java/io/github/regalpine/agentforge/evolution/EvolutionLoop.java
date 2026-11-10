package io.github.regalpine.agentforge.evolution;

import java.util.List;

/**
 * Evolution closed loop interface.
 * AF-ENHANCE-15 §50: Evolution Loop.
 *
 * <p>Observe → Evaluate → Identify
 * → Generate Candidate → Experiment
 * → Validate → Approve → Release
 * → Observe Again.
 */
public interface EvolutionLoop {

    /**
     * Execute one evolution cycle.
     */
    EvolutionCandidate execute(
        EvolutionProblem problem,
        EvolutionContext context,
        List<ValidationGate> gates
    );
}
