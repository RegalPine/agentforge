package io.github.regalpine.agentforge.evolution;

import java.util.List;

/**
 * Default evolution loop
 * implementation.
 * AF-ENHANCE-15 §50: Evolution Loop.
 */
public class DefaultEvolutionLoop
    implements EvolutionLoop {

    private final CandidateGenerator
        generator;

    public DefaultEvolutionLoop(
        CandidateGenerator generator
    ) {
        this.generator = generator;
    }

    @Override
    public EvolutionCandidate execute(
        EvolutionProblem problem,
        EvolutionContext context,
        List<ValidationGate> gates
    ) {
        var candidates =
            generator.generate(
                problem, context);

        if (candidates.isEmpty()) {
            throw new IllegalStateException(
                "no candidates generated"
            );
        }

        var candidate = candidates.get(0);

        for (var gate : gates) {
            var result =
                gate.validate(candidate);
            if (!result.passed()) {
                if (candidate
                    instanceof InMemoryEvolutionCandidate
                    mem) {
                    candidate = mem.reject();
                }
                return candidate;
            }
        }

        if (candidate
            instanceof InMemoryEvolutionCandidate
            mem) {
            candidate = mem.approve();
        }

        return candidate;
    }
}
