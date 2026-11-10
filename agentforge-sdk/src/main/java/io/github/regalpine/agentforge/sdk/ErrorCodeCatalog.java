package io.github.regalpine.agentforge.sdk;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Catalogue of all standard error codes.
 * AF-ENHANCE-04 §7: central registry of
 * error codes across all domains.
 *
 * <p>Each domain has a range of numbers:
 * <ul>
 *   <li>AGF-CORE-001..099 — Core / SDK</li>
 *   <li>AGF-MODEL-001..099 — Model Provider</li>
 *   <li>AGF-CAP-001..099 — Capability</li>
 *   <li>AGF-GRAPH-001..099 — Graph</li>
 *   <li>AGF-EXEC-001..099 — Execution</li>
 *   <li>AGF-RUNTIME-001..099 — Runtime</li>
 *   <li>AGF-MEM-001..099 — Memory</li>
 *   <li>AGF-RAG-001..099 — RAG / Knowledge</li>
 *   <li>AGF-MCP-001..099 — MCP</li>
 *   <li>AGF-SEC-001..099 — Security</li>
 *   <li>AGF-GOV-001..099 — Governance</li>
 *   <li>AGF-EVAL-001..099 — Evaluation</li>
 *   <li>AGF-CLI-001..099 — CLI</li>
 * </ul>
 */
public final class ErrorCodeCatalog {

    private static final Map<String, ErrorCode> CODES;

    static {
        Map<String, ErrorCode> m = new LinkedHashMap<>();

        // CORE
        register(m, ErrorCode.core(1,
            "Configuration error"));
        register(m, ErrorCode.core(2,
            "Bootstrap failed"));
        register(m, ErrorCode.core(3,
            "SDK lifecycle error"));
        register(m, ErrorCode.core(4,
            "Resource not found"));
        register(m, ErrorCode.core(5,
            "Duplicate registration"));

        // MODEL
        register(m, ErrorCode.model(1,
            "Model provider not available"));
        register(m, ErrorCode.model(2,
            "Authentication failed"));
        register(m, ErrorCode.model(3,
            "Rate limit exceeded"));
        register(m, ErrorCode.model(4,
            "Invalid response format"));
        register(m, ErrorCode.model(5,
            "Streaming error"));
        register(m, ErrorCode.model(6,
            "Embedding dimension mismatch"));

        // CAPABILITY
        register(m, ErrorCode.cap(1,
            "Capability not found"));
        register(m, ErrorCode.cap(2,
            "Capability invocation failed"));
        register(m, ErrorCode.cap(3,
            "Capability timeout"));
        register(m, ErrorCode.cap(4,
            "SSRF violation"));
        register(m, ErrorCode.cap(5,
            "SQL injection blocked"));
        register(m, ErrorCode.cap(6,
            "Path traversal blocked"));

        // GRAPH
        register(m, ErrorCode.graph(1,
            "Graph compilation failed"));
        register(m, ErrorCode.graph(2,
            "Invalid graph structure"));
        register(m, ErrorCode.graph(3,
            "Node binding error"));
        register(m, ErrorCode.graph(4,
            "Cycle detected"));

        // EXECUTION
        register(m, ErrorCode.exec(1,
            "Execution failed"));
        register(m, ErrorCode.exec(2,
            "Execution timeout"));
        register(m, ErrorCode.exec(3,
            "Execution cancelled"));
        register(m, ErrorCode.exec(4,
            "State recovery failed"));

        // RUNTIME
        register(m, ErrorCode.runtime(1,
            "Runtime initialisation failed"));
        register(m, ErrorCode.runtime(2,
            "Worker unavailable"));
        register(m, ErrorCode.runtime(3,
            "Checkpoint failed"));

        // MEMORY
        register(m, ErrorCode.mem(1,
            "Memory store error"));
        register(m, ErrorCode.mem(2,
            "Memory policy denied"));
        register(m, ErrorCode.mem(3,
            "Memory retrieval failed"));
        register(m, ErrorCode.mem(4,
            "Memory scope violation"));

        // RAG
        register(m, ErrorCode.rag(1,
            "Knowledge store error"));
        register(m, ErrorCode.rag(2,
            "Document parsing failed"));
        register(m, ErrorCode.rag(3,
            "Retrieval failed"));
        register(m, ErrorCode.rag(4,
            "Citation validation failed"));

        // MCP
        register(m, ErrorCode.mcp(1,
            "MCP connection failed"));
        register(m, ErrorCode.mcp(2,
            "MCP tool invocation failed"));
        register(m, ErrorCode.mcp(3,
            "MCP circuit breaker open"));

        // SECURITY
        register(m, ErrorCode.sec(1,
            "Access denied"));
        register(m, ErrorCode.sec(2,
            "Credential not found"));
        register(m, ErrorCode.sec(3,
            "Data classification violation"));
        register(m, ErrorCode.sec(4,
            "Audit log failure"));

        // GOVERNANCE
        register(m, ErrorCode.gov(1,
            "Policy evaluation failed"));
        register(m, ErrorCode.gov(2,
            "Budget exceeded"));
        register(m, ErrorCode.gov(3,
            "Rate limit exceeded"));

        // EVALUATION
        register(m, ErrorCode.evaluation(1,
            "Evaluation case not found"));
        register(m, ErrorCode.evaluation(2,
            "Evaluation execution failed"));

        // CLI
        register(m, ErrorCode.cli(1,
            "Unknown command"));
        register(m, ErrorCode.cli(2,
            "Invalid arguments"));
        register(m, ErrorCode.cli(3,
            "Project template error"));
        register(m, ErrorCode.cli(4,
            "Environment check failed"));

        CODES = Collections.unmodifiableMap(m);
    }

    private ErrorCodeCatalog() {}

    private static void register(
        Map<String, ErrorCode> m, ErrorCode code
    ) {
        m.put(code.code(), code);
    }

    /**
     * Look up an error code by its string.
     */
    public static ErrorCode lookup(String code) {
        return CODES.get(code);
    }

    /**
     * Get all registered error codes.
     */
    public static Map<String, ErrorCode> all() {
        return CODES;
    }

    /**
     * Get all error codes for a domain.
     */
    public static Map<String, ErrorCode> forDomain(
        String domain
    ) {
        Map<String, ErrorCode> result =
            new LinkedHashMap<>();

        String prefix = "AGF-" + domain + "-";

        for (var e : CODES.entrySet()) {
            if (e.getKey().startsWith(prefix)) {
                result.put(e.getKey(), e.getValue());
            }
        }

        return Collections.unmodifiableMap(result);
    }

    /**
     * Total number of registered codes.
     */
    public static int size() {
        return CODES.size();
    }
}
