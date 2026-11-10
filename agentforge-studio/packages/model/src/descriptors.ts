import type { Reference } from "./reference.js";

/**
 * ProviderDescriptor - maps to backend provider descriptor.
 */
export interface ProviderDescriptor {
  providerId: string;
  version: string;
  description?: string;
  capabilities: CapabilityDescriptor[];
  status: string;
}

/**
 * CapabilityDescriptor - maps to backend capability descriptor.
 */
export interface CapabilityDescriptor {
  capabilityId: string;
  capabilityVersion: string;
  description?: string;
  inputSchema?: JsonSchema;
  outputSchema?: JsonSchema;
  requiredPermissions?: string[];
}

/**
 * AgentDescriptor - maps to backend agent descriptor.
 */
export interface AgentDescriptor {
  agentId: string;
  version: string;
  name?: string;
  description?: string;
  capabilities: Reference[];
  status: string;
}

/**
 * JSON Schema subset for schema-driven forms.
 */
export interface JsonSchema {
  type: string;
  properties?: Record<string, JsonSchemaProperty>;
  required?: string[];
  items?: JsonSchema;
  description?: string;
  enum?: string[];
  default?: unknown;
}

/**
 * JSON Schema property definition.
 */
export interface JsonSchemaProperty {
  type: string;
  description?: string;
  enum?: string[];
  default?: unknown;
  items?: JsonSchema;
}
