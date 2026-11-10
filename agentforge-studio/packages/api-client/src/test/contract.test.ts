/**
 * CE-01: Contract tests verifying API Client endpoints match the OpenAPI spec.
 *
 * These tests intercept fetch calls made by AgentForgeApiClient and verify:
 * 1. URL paths match the OpenAPI spec (docs/api/openapi.yaml)
 * 2. HTTP methods match the spec (GET/POST/PUT/DELETE)
 * 3. Required headers (X-Tenant-Id, Authorization) are always sent
 * 4. Request bodies are sent for write operations
 *
 * Run with: pnpm --filter @agentforge/api-client test
 */

import { describe, it, expect, vi, beforeEach } from "vitest";
import { HttpClient } from "../http-client.js";
import { AgentForgeApiClient } from "../api-client.js";

/** Captured fetch call details. */
interface CapturedCall {
  url: string;
  method: string;
  headers: Record<string, string>;
  body: unknown;
}

function createMockedClient(): { client: AgentForgeApiClient; calls: CapturedCall[] } {
  const calls: CapturedCall[] = [];
  const mockFetch = vi.fn().mockImplementation(async (url: string, init: RequestInit) => {
    const headers: Record<string, string> = {};
    if (init?.headers) {
      for (const [k, v] of Object.entries(init.headers)) {
        headers[k] = String(v);
      }
    }
    calls.push({
      url: String(url),
      method: init?.method ?? "GET",
      headers,
      body: init?.body ? JSON.parse(String(init.body)) : undefined,
    });
    return {
      ok: true,
      status: 200,
      json: async () => ({}),
    };
  });
  vi.stubGlobal("fetch", mockFetch);

  const http = new HttpClient({
    baseUrl: "http://localhost:8080",
    tenantId: "test-tenant",
    authToken: "test-token",
  });
  const client = new AgentForgeApiClient(http);
  return { client, calls };
}

function lastCall(calls: CapturedCall[]): CapturedCall {
  return calls[calls.length - 1];
}

/**
 * Match a concrete URL against an OpenAPI path pattern.
 * e.g. "/api/v1/graphs/abc123" matches "/api/v1/graphs/{id}"
 */
function urlMatchesPattern(url: string, pattern: string, baseUrl: string): boolean {
  const path = url.replace(baseUrl, "").split("?")[0];
  const patternParts = pattern.split("/");
  const pathParts = path.split("/");
  if (patternParts.length !== pathParts.length) return false;
  return patternParts.every((part, i) => {
    if (part.startsWith("{") && part.endsWith("}")) return true;
    return part === pathParts[i];
  });
}

describe("CE-01: API Contract Tests", () => {
  let client: AgentForgeApiClient;
  let calls: CapturedCall[];
  const BASE = "http://localhost:8080";

  beforeEach(() => {
    const mocked = createMockedClient();
    client = mocked.client;
    calls = mocked.calls;
  });

  // ── Required headers contract ──

  describe("Required headers (OpenAPI §security)", () => {
    it("every request includes X-Tenant-Id header", async () => {
      await client.listGraphs();
      await client.getExecution("exec-1");
      await client.listAgentSummaries();

      for (const call of calls) {
        expect(call.headers["X-Tenant-Id"]).toBe("test-tenant");
      }
    });

    it("every request includes Authorization Bearer token", async () => {
      await client.listGraphs();
      await client.createGraph({} as never);

      for (const call of calls) {
        expect(call.headers["Authorization"]).toBe("Bearer test-token");
      }
    });

    it("every request includes Content-Type application/json", async () => {
      await client.listGraphs();
      await client.createGraph({} as never);

      for (const call of calls) {
        expect(call.headers["Content-Type"]).toBe("application/json");
      }
    });
  });

  // ── Graph endpoints contract ──

  describe("Graph endpoints (OpenAPI §paths.graphs)", () => {
    it("listGraphs → GET /api/v1/graphs", async () => {
      await client.listGraphs();
      const c = lastCall(calls);
      expect(c.method).toBe("GET");
      expect(c.url).toBe(`${BASE}/api/v1/graphs`);
    });

    it("createGraph → POST /api/v1/graphs with body", async () => {
      const graph = { id: "g1", version: "1.0", name: "Test" } as never;
      await client.createGraph(graph);
      const c = lastCall(calls);
      expect(c.method).toBe("POST");
      expect(c.url).toBe(`${BASE}/api/v1/graphs`);
      expect(c.body).toEqual(graph);
    });

    it("getGraph → GET /api/v1/graphs/{id}", async () => {
      await client.getGraph("abc");
      const c = lastCall(calls);
      expect(c.method).toBe("GET");
      expect(urlMatchesPattern(c.url, "/api/v1/graphs/{id}", BASE)).toBe(true);
      expect(c.url).toContain("/api/v1/graphs/abc");
    });

    it("getGraph with version → includes query param", async () => {
      await client.getGraph("abc", "2.0");
      const c = lastCall(calls);
      expect(c.url).toContain("version=2.0");
    });

    it("updateGraph → PUT /api/v1/graphs/{id}", async () => {
      const graph = { id: "abc", version: "1.0", name: "Updated" } as never;
      await client.updateGraph("abc", graph);
      const c = lastCall(calls);
      expect(c.method).toBe("PUT");
      expect(c.url).toContain("/api/v1/graphs/abc");
      expect(c.body).toEqual(graph);
    });

    it("deleteGraph → DELETE /api/v1/graphs/{id}", async () => {
      await client.deleteGraph("abc");
      const c = lastCall(calls);
      expect(c.method).toBe("DELETE");
      expect(c.url).toContain("/api/v1/graphs/abc");
    });

    it("validateGraph → POST /api/v1/graphs/validate", async () => {
      await client.validateGraph({} as never);
      const c = lastCall(calls);
      expect(c.method).toBe("POST");
      expect(c.url).toBe(`${BASE}/api/v1/graphs/validate`);
    });

    it("compileGraph → POST /api/v1/graphs/compile", async () => {
      await client.compileGraph({} as never);
      const c = lastCall(calls);
      expect(c.method).toBe("POST");
      expect(c.url).toBe(`${BASE}/api/v1/graphs/compile`);
    });
  });

  // ── Execution endpoints contract ──

  describe("Execution endpoints (OpenAPI §paths.executions)", () => {
    it("createExecution → POST /api/v1/executions with body", async () => {
      const req = { graphId: "g1", graphVersion: "1.0" };
      await client.createExecution(req);
      const c = lastCall(calls);
      expect(c.method).toBe("POST");
      expect(c.url).toBe(`${BASE}/api/v1/executions`);
      expect(c.body).toEqual(req);
    });

    it("getExecution → GET /api/v1/executions/{id}", async () => {
      await client.getExecution("e1");
      const c = lastCall(calls);
      expect(c.method).toBe("GET");
      expect(c.url).toContain("/api/v1/executions/e1");
    });

    it("getExecutionState → GET /api/v1/executions/{id}/state", async () => {
      await client.getExecutionState("e1");
      const c = lastCall(calls);
      expect(c.method).toBe("GET");
      expect(c.url).toContain("/api/v1/executions/e1/state");
    });

    it("listExecutions → GET /api/v1/executions", async () => {
      await client.listExecutions(10, 0);
      const c = lastCall(calls);
      expect(c.method).toBe("GET");
      expect(c.url).toContain("/api/v1/executions");
      expect(c.url).toContain("limit=10");
      expect(c.url).toContain("offset=0");
    });

    it("pauseExecution → POST /api/v1/executions/{id}/pause", async () => {
      await client.pauseExecution("e1");
      const c = lastCall(calls);
      expect(c.method).toBe("POST");
      expect(c.url).toContain("/api/v1/executions/e1/pause");
    });

    it("resumeExecution → POST /api/v1/executions/{id}/resume", async () => {
      await client.resumeExecution("e1");
      const c = lastCall(calls);
      expect(c.method).toBe("POST");
      expect(c.url).toContain("/api/v1/executions/e1/resume");
    });

    it("cancelExecution → POST /api/v1/executions/{id}/cancel", async () => {
      await client.cancelExecution("e1");
      const c = lastCall(calls);
      expect(c.method).toBe("POST");
      expect(c.url).toContain("/api/v1/executions/e1/cancel");
    });

    it("getExecutionEvents → GET /api/v1/executions/{id}/events", async () => {
      await client.getExecutionEvents("e1", 5);
      const c = lastCall(calls);
      expect(c.method).toBe("GET");
      expect(c.url).toContain("/api/v1/executions/e1/events");
      expect(c.url).toContain("after=5");
    });
  });

  // ── Agent endpoints contract ──

  describe("Agent endpoints (OpenAPI §paths.agents)", () => {
    it("listAgents → GET /api/v1/agents", async () => {
      await client.listAgents();
      const c = lastCall(calls);
      expect(c.method).toBe("GET");
      expect(c.url).toBe(`${BASE}/api/v1/agents`);
    });

    it("listAgentSummaries → GET /api/v1/agents/summaries", async () => {
      await client.listAgentSummaries();
      const c = lastCall(calls);
      expect(c.method).toBe("GET");
      expect(c.url).toBe(`${BASE}/api/v1/agents/summaries`);
    });

    it("getAgentOverview → GET /api/v1/agents/{agentId}/overview", async () => {
      await client.getAgentOverview("agent-1");
      const c = lastCall(calls);
      expect(c.method).toBe("GET");
      expect(c.url).toContain("/api/v1/agents/agent-1/overview");
    });

    it("getAgentDefinition → GET /api/v1/agents/{agentId}/definition", async () => {
      await client.getAgentDefinition("agent-1", "2.0");
      const c = lastCall(calls);
      expect(c.method).toBe("GET");
      expect(c.url).toContain("/api/v1/agents/agent-1/definition");
      expect(c.url).toContain("version=2.0");
    });

    it("listAgentVersions → GET /api/v1/agents/{agentId}/versions", async () => {
      await client.listAgentVersions("agent-1");
      const c = lastCall(calls);
      expect(c.method).toBe("GET");
      expect(c.url).toContain("/api/v1/agents/agent-1/versions");
    });
  });

  // ── Skill endpoints contract ──

  describe("Skill endpoints (OpenAPI §paths.skills)", () => {
    it("listSkillSummaries → GET /api/v1/skills", async () => {
      await client.listSkillSummaries();
      const c = lastCall(calls);
      expect(c.method).toBe("GET");
      expect(c.url).toBe(`${BASE}/api/v1/skills`);
    });

    it("invokeSkill → POST /api/v1/skills/invoke with body", async () => {
      const req = { skillId: "s1", input: { key: "val" } };
      await client.invokeSkill(req);
      const c = lastCall(calls);
      expect(c.method).toBe("POST");
      expect(c.url).toBe(`${BASE}/api/v1/skills/invoke`);
      expect(c.body).toEqual(req);
    });

    it("getSkillOverview → GET /api/v1/skills/{skillId}/overview", async () => {
      await client.getSkillOverview("s1");
      const c = lastCall(calls);
      expect(c.method).toBe("GET");
      expect(c.url).toContain("/api/v1/skills/s1/overview");
    });

    it("getSkillContract → GET /api/v1/skills/{skillId}/contract", async () => {
      await client.getSkillContract("s1", "1.0");
      const c = lastCall(calls);
      expect(c.method).toBe("GET");
      expect(c.url).toContain("/api/v1/skills/s1/contract");
      expect(c.url).toContain("version=1.0");
    });

    it("getSkillDependencies → GET /api/v1/skills/{skillId}/dependencies", async () => {
      await client.getSkillDependencies("s1");
      const c = lastCall(calls);
      expect(c.method).toBe("GET");
      expect(c.url).toContain("/api/v1/skills/s1/dependencies");
    });

    it("getSkillSecurity → GET /api/v1/skills/{skillId}/security", async () => {
      await client.getSkillSecurity("s1");
      const c = lastCall(calls);
      expect(c.method).toBe("GET");
      expect(c.url).toContain("/api/v1/skills/s1/security");
    });

    it("getSkillEvaluation → GET /api/v1/skills/{skillId}/evaluation", async () => {
      await client.getSkillEvaluation("s1");
      const c = lastCall(calls);
      expect(c.method).toBe("GET");
      expect(c.url).toContain("/api/v1/skills/s1/evaluation");
    });

    it("listSkillVersions → GET /api/v1/skills/{skillId}/versions", async () => {
      await client.listSkillVersions("s1");
      const c = lastCall(calls);
      expect(c.method).toBe("GET");
      expect(c.url).toContain("/api/v1/skills/s1/versions");
    });

    it("getSkillGovernance → GET /api/v1/skills/{skillId}/governance", async () => {
      await client.getSkillGovernance("s1");
      const c = lastCall(calls);
      expect(c.method).toBe("GET");
      expect(c.url).toContain("/api/v1/skills/s1/governance");
    });

    it("getSkillUsage → GET /api/v1/skills/{skillId}/usage", async () => {
      await client.getSkillUsage("s1");
      const c = lastCall(calls);
      expect(c.method).toBe("GET");
      expect(c.url).toContain("/api/v1/skills/s1/usage");
    });

    it("listSkillExecutions → GET /api/v1/skills/{skillId}/executions", async () => {
      await client.listSkillExecutions("s1", 5);
      const c = lastCall(calls);
      expect(c.method).toBe("GET");
      expect(c.url).toContain("/api/v1/skills/s1/executions");
      expect(c.url).toContain("limit=5");
    });

    it("transitionSkillLifecycle → PUT /api/v1/skills/{skillId}/versions/{version}/lifecycle", async () => {
      await client.transitionSkillLifecycle("s1", "1.0", "ACTIVE");
      const c = lastCall(calls);
      expect(c.method).toBe("PUT");
      expect(urlMatchesPattern(c.url, "/api/v1/skills/{skillId}/versions/{version}/lifecycle", BASE)).toBe(true);
      expect(c.body).toEqual({ targetState: "ACTIVE" });
    });
  });

  // ── OpenAPI coverage: all spec endpoints have client methods ──

  describe("OpenAPI coverage", () => {
    it("all OpenAPI spec endpoints are callable on the API client", async () => {
      // Verify each spec endpoint can be exercised.
      // This ensures no spec endpoint is missing from the client.
      const specMethods: Record<string, () => Promise<unknown>> = {
        "GET /api/v1/graphs": () => client.listGraphs(),
        "POST /api/v1/graphs": () => client.createGraph({} as never),
        "GET /api/v1/graphs/{id}": () => client.getGraph("test"),
        "PUT /api/v1/graphs/{id}": () => client.updateGraph("test", {} as never),
        "DELETE /api/v1/graphs/{id}": () => client.deleteGraph("test"),
        "POST /api/v1/graphs/validate": () => client.validateGraph({} as never),
        "POST /api/v1/graphs/compile": () => client.compileGraph({} as never),
        "POST /api/v1/executions": () => client.createExecution({ graphId: "g", graphVersion: "1" }),
        "GET /api/v1/executions": () => client.listExecutions(),
        "GET /api/v1/executions/{id}": () => client.getExecution("test"),
        "GET /api/v1/executions/{id}/state": () => client.getExecutionState("test"),
        "POST /api/v1/executions/{id}/pause": () => client.pauseExecution("test"),
        "POST /api/v1/executions/{id}/resume": () => client.resumeExecution("test"),
        "POST /api/v1/executions/{id}/cancel": () => client.cancelExecution("test"),
        "GET /api/v1/executions/{id}/events": () => client.getExecutionEvents("test"),
        "GET /api/v1/agents": () => client.listAgents(),
        "GET /api/v1/agents/summaries": () => client.listAgentSummaries(),
        "GET /api/v1/agents/{agentId}/overview": () => client.getAgentOverview("test"),
        "GET /api/v1/agents/{agentId}/definition": () => client.getAgentDefinition("test"),
        "GET /api/v1/agents/{agentId}/versions": () => client.listAgentVersions("test"),
        "GET /api/v1/skills": () => client.listSkillSummaries(),
        "POST /api/v1/skills/invoke": () => client.invokeSkill({ skillId: "s", input: {} }),
        "GET /api/v1/skills/{skillId}/overview": () => client.getSkillOverview("test"),
        "GET /api/v1/skills/{skillId}/contract": () => client.getSkillContract("test"),
        "GET /api/v1/skills/{skillId}/dependencies": () => client.getSkillDependencies("test"),
        "GET /api/v1/skills/{skillId}/security": () => client.getSkillSecurity("test"),
        "GET /api/v1/skills/{skillId}/evaluation": () => client.getSkillEvaluation("test"),
        "GET /api/v1/skills/{skillId}/versions": () => client.listSkillVersions("test"),
        "GET /api/v1/skills/{skillId}/governance": () => client.getSkillGovernance("test"),
        "GET /api/v1/skills/{skillId}/usage": () => client.getSkillUsage("test"),
        "GET /api/v1/skills/{skillId}/executions": () => client.listSkillExecutions("test"),
        "PUT /api/v1/skills/{skillId}/versions/{version}/lifecycle": () =>
          client.transitionSkillLifecycle("test", "1.0", "ACTIVE"),
      };

      // Verify every OpenAPI endpoint has a corresponding client method
      for (const [key, fn] of Object.entries(specMethods)) {
        calls.length = 0;
        await fn();
        expect(calls.length).toBeGreaterThan(0);
        // Verify the call matches the expected method
        const [expectedMethod] = key.split(" ");
        expect(lastCall(calls).method).toBe(expectedMethod);
      }
    });

    it("all endpoint URLs match their OpenAPI path patterns", async () => {
      // Exercise each endpoint and verify URL matches spec pattern
      const exercises: [string, string, () => Promise<unknown>][] = [
        ["GET", "/api/v1/graphs", () => client.listGraphs()],
        ["POST", "/api/v1/graphs", () => client.createGraph({} as never)],
        ["GET", "/api/v1/graphs/{id}", () => client.getGraph("x")],
        ["PUT", "/api/v1/graphs/{id}", () => client.updateGraph("x", {} as never)],
        ["DELETE", "/api/v1/graphs/{id}", () => client.deleteGraph("x")],
        ["POST", "/api/v1/graphs/validate", () => client.validateGraph({} as never)],
        ["POST", "/api/v1/graphs/compile", () => client.compileGraph({} as never)],
        ["POST", "/api/v1/executions", () => client.createExecution({ graphId: "g", graphVersion: "1" })],
        ["GET", "/api/v1/executions", () => client.listExecutions()],
        ["GET", "/api/v1/executions/{id}", () => client.getExecution("x")],
        ["GET", "/api/v1/executions/{id}/state", () => client.getExecutionState("x")],
        ["POST", "/api/v1/executions/{id}/pause", () => client.pauseExecution("x")],
        ["POST", "/api/v1/executions/{id}/resume", () => client.resumeExecution("x")],
        ["POST", "/api/v1/executions/{id}/cancel", () => client.cancelExecution("x")],
        ["GET", "/api/v1/executions/{id}/events", () => client.getExecutionEvents("x")],
        ["GET", "/api/v1/agents", () => client.listAgents()],
        ["GET", "/api/v1/agents/summaries", () => client.listAgentSummaries()],
        ["GET", "/api/v1/agents/{agentId}/overview", () => client.getAgentOverview("x")],
        ["GET", "/api/v1/agents/{agentId}/definition", () => client.getAgentDefinition("x")],
        ["GET", "/api/v1/agents/{agentId}/versions", () => client.listAgentVersions("x")],
        ["GET", "/api/v1/skills", () => client.listSkillSummaries()],
        ["POST", "/api/v1/skills/invoke", () => client.invokeSkill({ skillId: "s", input: {} })],
        ["GET", "/api/v1/skills/{skillId}/overview", () => client.getSkillOverview("x")],
        ["GET", "/api/v1/skills/{skillId}/contract", () => client.getSkillContract("x")],
        ["GET", "/api/v1/skills/{skillId}/dependencies", () => client.getSkillDependencies("x")],
        ["GET", "/api/v1/skills/{skillId}/security", () => client.getSkillSecurity("x")],
        ["GET", "/api/v1/skills/{skillId}/evaluation", () => client.getSkillEvaluation("x")],
        ["GET", "/api/v1/skills/{skillId}/versions", () => client.listSkillVersions("x")],
        ["GET", "/api/v1/skills/{skillId}/governance", () => client.getSkillGovernance("x")],
        ["GET", "/api/v1/skills/{skillId}/usage", () => client.getSkillUsage("x")],
        ["GET", "/api/v1/skills/{skillId}/executions", () => client.listSkillExecutions("x")],
        ["PUT", "/api/v1/skills/{skillId}/versions/{version}/lifecycle", () => client.transitionSkillLifecycle("x", "1", "ACTIVE")],
      ];

      for (const [method, pattern, fn] of exercises) {
        calls.length = 0;
        await fn();
        const c = lastCall(calls);
        expect(c.method).toBe(method);
        expect(urlMatchesPattern(c.url, pattern, BASE)).toBe(true);
      }
    });
  });

  // ── Schema field contract (compile-time via ApiSchemas) ──

  describe("Schema field contracts (compile-time via ApiSchemas)", () => {
    it("GraphSummary has required fields per OpenAPI spec", () => {
      // This test verifies at compile-time that the type has the expected fields.
      // If the OpenAPI spec changes and types are regenerated, this will fail to compile.
      const summary: import("@agentforge/model").ApiSchemas["GraphSummary"] = {
        id: "g1",
        version: "1.0",
        name: "Test",
        status: "DRAFT",
        nodeCount: 3,
        edgeCount: 2,
        createdAt: "2026-01-01T00:00:00Z",
      };
      expect(summary.id).toBe("g1");
      expect(summary.status).toBe("DRAFT");
    });

    it("Execution has required fields per OpenAPI spec", () => {
      const execution: import("@agentforge/model").ApiSchemas["Execution"] = {
        id: "e1",
        graphId: "g1",
        graphVersion: "1.0",
        status: "RUNNING",
        tenantId: "t1",
        createdAt: "2026-01-01T00:00:00Z",
      };
      expect(execution.id).toBe("e1");
      expect(execution.status).toBe("RUNNING");
    });

    it("ExecutionHandle has required fields per OpenAPI spec", () => {
      const handle: import("@agentforge/model").ApiSchemas["ExecutionHandle"] = {
        executionId: "e1",
        status: "PENDING",
      };
      expect(handle.executionId).toBe("e1");
    });

    it("AgentSummaryView has required fields per OpenAPI spec", () => {
      const agent: import("@agentforge/model").ApiSchemas["AgentSummaryView"] = {
        id: "a1",
        name: "Agent",
        version: "1.0",
        status: "ACTIVE",
      };
      expect(agent.id).toBe("a1");
    });

    it("SkillSummaryView has required fields per OpenAPI spec", () => {
      const skill: import("@agentforge/model").ApiSchemas["SkillSummaryView"] = {
        id: "s1",
        name: "Skill",
        version: "1.0",
        lifecycleState: "DRAFT",
      };
      expect(skill.id).toBe("s1");
      expect(skill.lifecycleState).toBe("DRAFT");
    });

    it("SkillInvocationRequest has required fields per OpenAPI spec", () => {
      const req: import("@agentforge/model").ApiSchemas["SkillInvocationRequest"] = {
        skillId: "s1",
        input: { key: "val" },
      };
      expect(req.skillId).toBe("s1");
    });
  });
});
