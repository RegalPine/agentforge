package io.github.regalpine.agentforge.multiagent.result;

import io.github.regalpine.agentforge.multiagent.contract.AgentContract;

import java.util.List;

/**
 * ResultValidator — AE-11 §25.
 *
 * Validates child agent results against the agent contract.
 * Ensures:
 * - Result status is consistent with completeness
 * - Partial results are not marked as complete
 * - Failed results have error codes
 */
public final class ResultValidator {

    /**
     * Validate a result against the contract.
     *
     * @return list of validation errors (empty = valid)
     */
    public <T> List<String> validate(
        AgentResult<T> result,
        AgentContract contract
    ) {
        if (result == null) {
            return List.of("Result is null");
        }

        var errors = new java.util.ArrayList<String>();

        // Partial must not be complete
        if (result.status() == ResultStatus.PARTIAL
            && result.complete()
        ) {
            errors.add(
                "Partial result must not be marked complete"
            );
        }

        // Success should be complete
        if (result.status() == ResultStatus.SUCCESS
            && !result.complete()
        ) {
            errors.add(
                "Success result should be marked complete"
            );
        }

        // Failed must have error code
        if (result.status() == ResultStatus.FAILED
            && (result.errorCode() == null
                || result.errorCode().isBlank())
        ) {
            errors.add(
                "Failed result must have an error code"
            );
        }

        // Policy denied must have reason
        if (result.status() == ResultStatus.POLICY_DENIED
            && (result.errorMessage() == null
                || result.errorMessage().isBlank())
        ) {
            errors.add(
                "Policy denied must have a reason"
            );
        }

        // Provenance must have child execution id
        if (result.provenance() == null) {
            errors.add("Result must have provenance");
        }

        return List.copyOf(errors);
    }
}
