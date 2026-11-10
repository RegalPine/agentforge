package io.github.regalpine.agentforge.multiagent.projection;

import io.github.regalpine.agentforge.multiagent.contract.AgentContract;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * DefaultContextProjection — AE-11 §8, §50.
 *
 * Default projection strategy:
 * - ALLOW: task, scope, userInput, businessParams,
 *   selectedState, selectedObservation, selectedMemory,
 *   selectedKnowledge, executionId, tenantId
 * - BLOCK: systemPrompt, credentials, secrets,
 *   apiKeys, oauthTokens, internalPolicy,
 *   parentReasoning, privateContext
 *
 * <p>Keys are matched against a configurable allowlist
 * and blocklist. Blocked keys are never passed.
 */
public final class DefaultContextProjection
    implements ContextProjection {

    private static final Set<String> DEFAULT_ALLOWED = Set.of(
        "task", "scope", "userInput",
        "businessParams", "selectedState",
        "selectedObservation", "selectedMemory",
        "selectedKnowledge", "executionId",
        "tenantId", "parameters", "input",
        "locale", "sessionId"
    );

    private static final Set<String> BLOCKED = Set.of(
        "systemPrompt", "credentials", "secrets",
        "apiKeys", "oauthTokens", "internalPolicy",
        "parentReasoning", "privateContext",
        "password", "token", "secret",
        "apiKey", "authorization"
    );

    private final Set<String> allowedKeys;

    public DefaultContextProjection() {
        this(DEFAULT_ALLOWED);
    }

    public DefaultContextProjection(Set<String> allowedKeys) {
        this.allowedKeys = Set.copyOf(allowedKeys);
    }

    @Override
    public ProjectedContext project(
        Map<String, Object> parentContext,
        AgentContract childContract
    ) {
        if (parentContext == null || parentContext.isEmpty()) {
            return ProjectedContext.empty();
        }

        Map<String, Object> projected = new HashMap<>();
        boolean redacted = false;

        for (Map.Entry<String, Object> entry
            : parentContext.entrySet()
        ) {
            String key = entry.getKey();

            if (BLOCKED.contains(key)) {
                redacted = true;
                continue;
            }

            if (allowedKeys.contains(key)) {
                projected.put(key, entry.getValue());
            }
        }

        return new ProjectedContext(
            Map.copyOf(projected),
            redacted
        );
    }
}
