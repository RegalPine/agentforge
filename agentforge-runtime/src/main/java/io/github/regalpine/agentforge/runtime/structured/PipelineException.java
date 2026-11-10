package io.github.regalpine.agentforge.runtime.structured;

/**
 * Exception thrown during pipeline processing.
 */
public class PipelineException extends RuntimeException {

    private final String stageName;

    public PipelineException(String stageName, String message) {
        super("Pipeline stage '" + stageName + "' failed: " + message);
        this.stageName = stageName;
    }

    public PipelineException(String stageName, String message, Throwable cause) {
        super("Pipeline stage '" + stageName + "' failed: " + message, cause);
        this.stageName = stageName;
    }

    public String getStageName() {
        return stageName;
    }
}
