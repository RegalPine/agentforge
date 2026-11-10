package io.github.regalpine.agentforge.sdk;

/** Thrown when SDK configuration is invalid. */
public class ConfigurationException extends AgentForgeException {
    public ConfigurationException(String message) {
        super("CONFIGURATION_ERROR", message);
    }

    public ConfigurationException(String message, Throwable cause) {
        super("CONFIGURATION_ERROR", message, cause);
    }
}
