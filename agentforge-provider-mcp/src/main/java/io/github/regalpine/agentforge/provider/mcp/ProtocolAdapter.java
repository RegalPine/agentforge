package io.github.regalpine.agentforge.provider.mcp;

/**
 * Protocol adapter SPI.
 * AF-ENHANCE-10 §66: ProtocolAdapter.
 *
 * <p>Unified interface for all
 * protocol adapters (MCP, A2A).
 */
public interface ProtocolAdapter {

    /**
     * Protocol name (e.g. "mcp", "a2a").
     */
    String protocol();

    /**
     * Protocol version.
     */
    ProtocolVersion version();

    /**
     * Protocol capabilities.
     */
    ProtocolCapabilities capabilities();
}
