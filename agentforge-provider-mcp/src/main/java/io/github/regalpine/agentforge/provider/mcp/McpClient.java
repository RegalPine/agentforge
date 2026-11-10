package io.github.regalpine.agentforge.provider.mcp;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletionStage;

/**
 * Enhanced MCP client interface.
 * AF-ENHANCE-10 §7: McpClient.
 *
 * <p>Provides server discovery,
 * tool/resource/prompt discovery,
 * and tool invocation.
 */
public interface McpClient {

    /**
     * Server identifier.
     */
    McpServerId serverId();

    /**
     * Server information.
     */
    McpServerInfo serverInfo();

    /**
     * Current lifecycle state.
     */
    McpLifecycle lifecycle();

    /**
     * Discovered tools.
     */
    List<McpToolDescriptor> tools();

    /**
     * Discovered resources.
     */
    List<McpResourceDefinition> resources();

    /**
     * Discovered prompts.
     */
    List<McpPromptDefinition> prompts();

    /**
     * Call a tool by name.
     */
    CompletionStage<McpToolResult> callTool(
        String name,
        java.util.Map<String, Object> arguments
    );

    /**
     * Read a resource by URI.
     */
    CompletionStage<String> readResource(
        String uri
    );

    /**
     * Get a prompt by name.
     */
    CompletionStage<McpPromptDefinition> getPrompt(
        String name
    );
}
