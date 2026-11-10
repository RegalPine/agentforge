/**
 * Service Channel viewer models.
 * AF-ENHANCE-11 §3: AgentService / Session / Conversation.
 *
 * Maps to backend agentforge-service types.
 */

// ── Agent Service ──

/**
 * AgentServiceView - registered service overview.
 */
export interface AgentServiceView {
  readonly serviceId: string;
  readonly namespace: string;
  readonly name: string;
  readonly version: string;
  readonly status: AgentServiceStatus;
  readonly sessionCount: number;
  readonly createdAt: string;
}

export type AgentServiceStatus =
  | "ACTIVE"
  | "INACTIVE"
  | "DEGRADED"
  | "UNREGISTERED";

// ── Session ──

/**
 * SessionView - active session detail.
 */
export interface SessionView {
  readonly sessionId: string;
  readonly tenantId: string;
  readonly status: SessionStatusView;
  readonly conversationCount: number;
  readonly createdAt: string;
  readonly activatedAt?: string;
  readonly closedAt?: string;
}

export type SessionStatusView =
  | "CREATED"
  | "ACTIVE"
  | "SUSPENDED"
  | "RESUMING"
  | "CLOSING"
  | "CLOSED"
  | "EXPIRED";

// ── Conversation ──

/**
 * ConversationView - conversation within a session.
 */
export interface ConversationView {
  readonly conversationId: string;
  readonly sessionId: string;
  readonly messageCount: number;
  readonly messages: ConversationMessageView[];
  readonly createdAt: string;
}

export interface ConversationMessageView {
  readonly messageId: string;
  readonly role: MessageRoleView;
  readonly content: string;
  readonly timestamp: string;
  readonly metadata?: Record<string, string>;
}

export type MessageRoleView =
  | "USER"
  | "ASSISTANT"
  | "SYSTEM"
  | "TOOL"
  | "AGENT"
  | "OBSERVER";

// ── Channel ──

/**
 * ChannelTypeView - service channel type.
 */
export type ChannelTypeView =
  | "SYNC"
  | "ASYNC"
  | "STREAMING"
  | "BATCH"
  | "EVENT"
  | "PUBSUB"
  | "WEBSOCKET";

// ── Service Health ──

/**
 * ServiceHealthView - per-service health.
 */
export interface ServiceHealthView {
  readonly serviceId: string;
  readonly channelType: ChannelTypeView;
  readonly activeSessions: number;
  readonly totalRequests: number;
  readonly avgLatencyMs: number;
  readonly errorRate: number;
  readonly lastHealthCheck: string;
}
