package io.github.regalpine.agentforge.server.bootstrap;

import io.github.regalpine.agentforge.kernel.config.AgentForgeConfig;

import java.util.ArrayList;
import java.util.List;

/**
 * G4-1: Validates that the configuration meets the
 * requirements for the target deployment mode.
 *
 * <p>In production mode, shared persistence and proper
 * security must be configured. This validator fails
 * fast with clear error messages if requirements are
 * not met.
 */
public final class ProductionAssemblyValidator {

    /**
     * Validate the configuration for the given mode.
     *
     * @throws IllegalStateException if validation fails
     */
    public static void validate(
        AgentForgeConfig config,
        DeploymentMode mode
    ) {
        if (mode == DeploymentMode.DEMO) {
            return;
        }

        List<String> violations = new ArrayList<>();

        // Database must not be in-memory for production
        AgentForgeConfig.DatabaseConfig db = config.database();
        if (db == null || "memory".equalsIgnoreCase(db.type())) {
            violations.add(
                "Production mode requires a shared database. "
                + "Set database.type to 'postgresql' and "
                + "configure database.url."
            );
        } else if ("postgresql".equalsIgnoreCase(db.type())) {
            if (db.url() == null || db.url().isBlank()) {
                violations.add(
                    "Production PostgreSQL requires database.url."
                );
            }
        }

        // Admin password must be explicitly set
        AgentForgeConfig.SecurityConfig sec = config.security();
        if (sec != null) {
            if (sec.skipPasswordCheck()) {
                violations.add(
                    "Production mode must not skip password check. "
                    + "Set AGENTFORGE_SKIP_PASSWORD_CHECK=false."
                );
            }
            if (sec.adminPassword() == null
                || sec.adminPassword().isBlank()) {
                violations.add(
                    "Production mode requires an explicit admin "
                    + "password via AGENTFORGE_ADMIN_PASSWORD."
                );
            }
        }

        // Redis URI must be configured for distributed coordination
        AgentForgeConfig.RedisConfig redis = config.redis();
        if (redis == null || redis.uri() == null || redis.uri().isBlank()) {
            violations.add(
                "Production mode requires Redis URI for distributed "
                + "coordination. Set AGENTFORGE_REDIS_URI."
            );
        }

        if (!violations.isEmpty()) {
            throw new IllegalStateException(
                "Production assembly validation failed:\n  - "
                + String.join("\n  - ", violations)
            );
        }
    }
}
