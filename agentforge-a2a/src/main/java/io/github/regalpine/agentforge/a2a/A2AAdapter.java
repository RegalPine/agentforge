package io.github.regalpine.agentforge.a2a;

/**
 * A2A adapter interface.
 * AF-ENHANCE-10 §68: A2AAdapter.
 *
 * <p>Extends ProtocolAdapter with
 * A2A-specific connect/expose.
 */
public interface A2AAdapter
    extends io.github.regalpine.agentforge.provider.mcp.ProtocolAdapter {

    /**
     * Connect to an external agent.
     */
    A2AClient connect(
        ExternalAgentReference agent,
        io.github.regalpine.agentforge.provider.mcp.ProtocolContext context
    );

    @Override
    default String protocol() {
        return "a2a";
    }
}
