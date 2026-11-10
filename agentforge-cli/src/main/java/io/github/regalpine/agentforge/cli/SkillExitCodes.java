package io.github.regalpine.agentforge.cli;

/**
 * CLI 退出码契约.
 * AF-SKILL-04 §43: Exit Code Contract.
 *
 * <p>All skill commands follow a unified
 * exit code convention:
 * <ul>
 *   <li>0: Success</li>
 *   <li>1: General error</li>
 *   <li>2: Usage error (bad args)</li>
 *   <li>3: Skill not found</li>
 *   <li>4: Security/Policy denied</li>
 *   <li>5: Lifecycle transition error</li>
 *   <li>10: Network/Server error</li>
 * </ul>
 */
public final class SkillExitCodes {

    public static final int SUCCESS = 0;
    public static final int GENERAL_ERROR = 1;
    public static final int USAGE_ERROR = 2;
    public static final int SKILL_NOT_FOUND = 3;
    public static final int SECURITY_DENIED = 4;
    public static final int LIFECYCLE_ERROR = 5;
    public static final int NETWORK_ERROR = 10;

    private SkillExitCodes() {
        // utility class
    }

    /**
     * Create a CliOutput with the given
     * exit code.
     */
    public static CliOutput withCode(
        int exitCode,
        String message
    ) {
        return new CliOutput(exitCode, message);
    }
}
