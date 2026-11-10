/**
 * MCP workspace viewer models.
 * AF-STUDIO-01 §34-35: MCP Workspace, MCP Security.
 *
 * Maps to backend AE-10 MCP types.
 */

// ── MCP Connection ──

export type McpTransportType = "STDIO" | "STREAMABLE_HTTP";

export type McpConnectionStatus =
  | "CREATED"
  | "CONNECTING"
  | "CONNECTED"
  | "READY"
  | "FAILED"
  | "DISCONNECTED"
  | "RECONNECTING";

export interface McpConnectionView {
  readonly connectionId: string;
  readonly serverId: string;
  readonly endpoint: string;
  readonly transport: McpTransportType;
  readonly status: McpConnectionStatus;
  readonly credentialReference: string;
  readonly lastConnectedAt?: string;
  readonly lastError?: string;
  readonly metadata: Record<string, unknown>;
}

// ── MCP Server ──

export interface McpServerView {
  readonly serverId: string;
  readonly name: string;
  readonly description: string;
  readonly version: string;
  readonly connection: McpConnectionView;
  readonly toolCount: number;
  readonly resourceCount: number;
  readonly promptCount: number;
  readonly health: McpHealthStatus;
  readonly metadata: Record<string, unknown>;
}

export type McpHealthStatus = "HEALTHY" | "DEGRADED" | "UNAVAILABLE" | "UNKNOWN";

// ── MCP Tool ──

export interface McpToolView {
  readonly toolId: string;
  readonly serverId: string;
  readonly name: string;
  readonly description: string;
  readonly inputSchema: Record<string, unknown>;
  readonly outputSchema: Record<string, unknown>;
  readonly registrationStatus: McpToolRegistrationStatus;
  readonly invocationCount: number;
  readonly lastInvokedAt?: string;
  readonly metadata: Record<string, unknown>;
}

/**
 * McpToolRegistrationStatus - discovery ≠ enablement.
 * AF-STUDIO-01 §35: Discovered ≠ Enabled.
 */
export type McpToolRegistrationStatus =
  | "DISCOVERED"
  | "REGISTERED"
  | "ENABLED"
  | "DISABLED"
  | "REVOKED";

// ── MCP Resource ──

export interface McpResourceView {
  readonly resourceId: string;
  readonly serverId: string;
  readonly uri: string;
  readonly name: string;
  readonly description: string;
  readonly mimeType: string;
}

// ── MCP Prompt ──

export interface McpPromptView {
  readonly promptId: string;
  readonly serverId: string;
  readonly name: string;
  readonly description: string;
  readonly arguments: McpPromptArgument[];
}

export interface McpPromptArgument {
  readonly name: string;
  readonly description: string;
  readonly required: boolean;
}
