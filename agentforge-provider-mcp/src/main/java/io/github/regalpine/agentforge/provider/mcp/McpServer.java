package io.github.regalpine.agentforge.provider.mcp;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletionStage;

/**
 * MCP server interface.
 * AF-ENHANCE-10 §8: McpServer.
 *
 * <p>AgentForge can also expose
 * its own capabilities via MCP.
 */
public interface McpServer {

    /**
     * Server information.
     */
    McpServerInfo info();

    /**
     * Available tools.
     */
    List<McpToolDescriptor> tools();

    /**
     * Available resources.
     */
    List<McpResourceDefinition> resources();

    /**
     * Available prompts.
     */
    List<McpPromptDefinition> prompts();

    /**
     * Invoke a tool.
     */
    CompletionStage<McpToolResult> invoke(
        McpToolInvocation call
    );
}
