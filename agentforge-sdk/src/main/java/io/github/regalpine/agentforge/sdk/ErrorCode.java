package io.github.regalpine.agentforge.sdk;

import java.util.Objects;

/**
 * Structured error code.
 * AF-ENHANCE-04 §7: every error in
 * AgentForge carries a structured code
 * with domain, number, description,
 * context and suggested action.
 *
 * <p>Format: AGF-{DOMAIN}-{NUMBER}
 * e.g. AGF-MODEL-001, AGF-CAP-003
 */
public record ErrorCode(
    String domain,
    int number,
    String description,
    String suggestedAction
) {

    public ErrorCode {
        Objects.requireNonNull(domain, "domain");
        Objects.requireNonNull(description, "description");

        if (number < 0) {
            number = 0;
        }

        suggestedAction =
            suggestedAction == null
                ? ""
                : suggestedAction;
    }

    /**
     * Full error code string.
     * e.g. "AGF-MODEL-001"
     */
    public String code() {
        return "AGF-" + domain + "-"
            + String.format("%03d", number);
    }

    @Override
    public String toString() {
        return code() + ": " + description;
    }

    // --- Factory methods for all domains ---

    // CORE
    public static ErrorCode core(int n, String desc) {
        return new ErrorCode("CORE", n, desc, null);
    }

    // MODEL
    public static ErrorCode model(int n, String desc) {
        return new ErrorCode("MODEL", n, desc, null);
    }

    // CAPABILITY
    public static ErrorCode cap(int n, String desc) {
        return new ErrorCode("CAP", n, desc, null);
    }

    // GRAPH
    public static ErrorCode graph(int n, String desc) {
        return new ErrorCode("GRAPH", n, desc, null);
    }

    // EXECUTION
    public static ErrorCode exec(int n, String desc) {
        return new ErrorCode("EXEC", n, desc, null);
    }

    // RUNTIME
    public static ErrorCode runtime(int n, String desc) {
        return new ErrorCode("RUNTIME", n, desc, null);
    }

    // MEMORY
    public static ErrorCode mem(int n, String desc) {
        return new ErrorCode("MEM", n, desc, null);
    }

    // RAG / Knowledge
    public static ErrorCode rag(int n, String desc) {
        return new ErrorCode("RAG", n, desc, null);
    }

    // MCP
    public static ErrorCode mcp(int n, String desc) {
        return new ErrorCode("MCP", n, desc, null);
    }

    // SECURITY
    public static ErrorCode sec(int n, String desc) {
        return new ErrorCode("SEC", n, desc, null);
    }

    // GOVERNANCE
    public static ErrorCode gov(int n, String desc) {
        return new ErrorCode("GOV", n, desc, null);
    }

    // EVALUATION
    public static ErrorCode evaluation(int n, String desc) {
        return new ErrorCode("EVAL", n, desc, null);
    }

    // CLI
    public static ErrorCode cli(int n, String desc) {
        return new ErrorCode("CLI", n, desc, null);
    }
}
