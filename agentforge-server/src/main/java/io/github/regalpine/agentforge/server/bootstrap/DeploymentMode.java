package io.github.regalpine.agentforge.server.bootstrap;

/**
 * G4-1: Deployment mode distinguishing single-node demo
 * from production multi-replica assembly.
 *
 * <p>In {@link #DEMO} mode, in-memory stores and mock data
 * are acceptable. In {@link #PRODUCTION} mode, shared
 * persistence and real service implementations are required.
 */
public enum DeploymentMode {

    /**
     * Single-node demo for local development and testing.
     * In-memory stores, mock data, and relaxed security
     * are permitted.
     */
    DEMO,

    /**
     * Production multi-replica deployment.
     * Requires shared database, real service implementations,
     * and proper security configuration.
     */
    PRODUCTION;

    /**
     * Resolve from a string value (case-insensitive).
     * Defaults to {@link #DEMO} if null or unrecognised.
     */
    public static DeploymentMode fromString(String value) {
        if (value == null) return DEMO;
        return switch (value.trim().toLowerCase()) {
            case "production", "prod" -> PRODUCTION;
            default -> DEMO;
        };
    }
}
