package io.github.regalpine.agentforge.sdk;

import java.util.Map;
import java.util.Objects;

/**
 * Base exception for all AgentForge SDK operations.
 * <p>
 * All SDK exceptions carry contextual information (executionId, agentId, etc.)
 * for diagnostics and observability.
 */
public class AgentForgeException extends RuntimeException {

    private final String errorCode;
    private final String executionId;
    private final String agentId;
    private final String capabilityId;
    private final String graphId;
    private final boolean retryable;
    private final Map<String, Object> metadata;

    public AgentForgeException(String errorCode, String message) {
        this(errorCode, message, null, null, null, null, false, Map.of());
    }

    public AgentForgeException(String errorCode, String message, Throwable cause) {
        this(errorCode, message, cause, null, null, null, false, Map.of());
    }

    public AgentForgeException(
        String errorCode,
        String message,
        Throwable cause,
        String executionId,
        String agentId,
        String capabilityId,
        boolean retryable,
        Map<String, Object> metadata
    ) {
        super(message, cause);
        this.errorCode = Objects.requireNonNull(errorCode, "errorCode cannot be null");
        this.executionId = executionId;
        this.agentId = agentId;
        this.capabilityId = capabilityId;
        this.graphId = null;
        this.retryable = retryable;
        this.metadata = metadata == null ? Map.of() : Map.copyOf(metadata);
    }

    public String errorCode() {
        return errorCode;
    }

    public String executionId() {
        return executionId;
    }

    public String agentId() {
        return agentId;
    }

    public String capabilityId() {
        return capabilityId;
    }

    public String graphId() {
        return graphId;
    }

    public boolean isRetryable() {
        return retryable;
    }

    public Map<String, Object> metadata() {
        return metadata;
    }
}
