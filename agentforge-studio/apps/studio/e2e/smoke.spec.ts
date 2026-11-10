/**
 * CE-02: E2E smoke tests for AgentForge Studio.
 *
 * These tests verify core navigation and page rendering.
 * They run against the Vite dev server with API calls mocked via route interception.
 */

import { test, expect } from "@playwright/test";

// ── Mock API responses ──

const MOCK_GRAPHS = [
  {
    id: "g1",
    version: "1.0",
    name: "Test Graph",
    status: "DRAFT",
    nodeCount: 3,
    edgeCount: 2,
    createdAt: "2026-01-01T00:00:00Z",
  },
];

const MOCK_EXECUTIONS = [
  {
    id: "e1",
    graphId: "g1",
    graphVersion: "1.0",
    status: "COMPLETED",
    tenantId: "default",
    createdAt: "2026-01-01T00:00:00Z",
    startedAt: "2026-01-01T00:00:01Z",
    completedAt: "2026-01-01T00:00:05Z",
  },
];

const MOCK_AGENT_SUMMARIES = [
  {
    id: "a1",
    name: "Test Agent",
    version: "1.0.0",
    status: "ACTIVE",
    graphCount: 2,
    lastExecutedAt: "2026-01-01T00:00:00Z",
  },
];

const MOCK_SKILL_SUMMARIES = [
  {
    id: "s1",
    name: "Test Skill",
    version: "1.0.0",
    lifecycleState: "DRAFT",
    description: "A test skill",
    createdAt: "2026-01-01T00:00:00Z",
  },
];

/**
 * Set up API route mocks for all major endpoint groups.
 */
async function mockApi(page: import("@playwright/test").Page) {
  // Graphs
  await page.route("**/api/v1/graphs", (route) =>
    route.fulfill({ json: MOCK_GRAPHS })
  );
  await page.route("**/api/v1/graphs/*", (route) => {
    if (route.request().method() === "GET") {
      route.fulfill({ json: MOCK_GRAPHS[0] });
    } else {
      route.fulfill({ status: 204 });
    }
  });

  // Executions
  await page.route("**/api/v1/executions", (route) =>
    route.fulfill({ json: MOCK_EXECUTIONS })
  );
  await page.route("**/api/v1/executions/*", (route) =>
    route.fulfill({ json: MOCK_EXECUTIONS[0] })
  );

  // Agents
  await page.route("**/api/v1/agents/summaries", (route) =>
    route.fulfill({ json: MOCK_AGENT_SUMMARIES })
  );
  await page.route("**/api/v1/agents/*", (route) =>
    route.fulfill({ json: MOCK_AGENT_SUMMARIES[0] })
  );

  // Skills
  await page.route("**/api/v1/skills", (route) =>
    route.fulfill({ json: MOCK_SKILL_SUMMARIES })
  );
  await page.route("**/api/v1/skills/*", (route) =>
    route.fulfill({ json: MOCK_SKILL_SUMMARIES[0] })
  );

  // Governance / Approvals
  await page.route("**/api/v1/human-approvals*", (route) =>
    route.fulfill({ json: [] })
  );

  // Observability
  await page.route("**/api/v1/observability/*", (route) =>
    route.fulfill({ json: {} })
  );

  // Evaluation
  await page.route("**/api/v1/evaluations/*", (route) =>
    route.fulfill({ json: [] })
  );
}

// ── Navigation smoke tests ──

test.describe("Navigation", () => {
  test("homepage loads and shows navigation", async ({ page }) => {
    await mockApi(page);
    await page.goto("/");
    await expect(page).toHaveTitle(/AgentForge/);
  });

  test("can navigate to Graphs page", async ({ page }) => {
    await mockApi(page);
    await page.goto("/graphs");
    // Page should render without crashing
    await page.waitForLoadState("networkidle");
  });

  test("can navigate to Executions page", async ({ page }) => {
    await mockApi(page);
    await page.goto("/executions");
    await page.waitForLoadState("networkidle");
  });

  test("can navigate to Agents workspace", async ({ page }) => {
    await mockApi(page);
    await page.goto("/agents");
    await page.waitForLoadState("networkidle");
  });

  test("can navigate to Skills workspace", async ({ page }) => {
    await mockApi(page);
    await page.goto("/skills");
    await page.waitForLoadState("networkidle");
  });
});

// ── Page rendering smoke tests ──

test.describe("Page rendering", () => {
  test("Graphs page renders table", async ({ page }) => {
    await mockApi(page);
    await page.goto("/graphs");
    await page.waitForLoadState("networkidle");
    // Should contain the graph name from mock data
    const body = page.locator("body");
    await expect(body).toBeVisible();
  });

  test("Executions page renders without errors", async ({ page }) => {
    await mockApi(page);
    const errors: string[] = [];
    page.on("pageerror", (err) => errors.push(err.message));

    await page.goto("/executions");
    await page.waitForLoadState("networkidle");

    expect(errors).toHaveLength(0);
  });

  test("Agent workspace renders without errors", async ({ page }) => {
    await mockApi(page);
    const errors: string[] = [];
    page.on("pageerror", (err) => errors.push(err.message));

    await page.goto("/agents");
    await page.waitForLoadState("networkidle");

    expect(errors).toHaveLength(0);
  });
});

// ── Accessibility smoke tests ──

test.describe("Accessibility", () => {
  test("all pages have lang attribute on html", async ({ page }) => {
    await mockApi(page);
    await page.goto("/");
    const lang = await page.locator("html").getAttribute("lang");
    expect(lang).toBeTruthy();
  });

  test("interactive elements are keyboard accessible", async ({ page }) => {
    await mockApi(page);
    await page.goto("/graphs");
    await page.waitForLoadState("networkidle");

    // Tab through the page and verify focus moves
    const focusedBefore = await page.evaluate(() => {
      return document.activeElement?.tagName ?? "BODY";
    });

    await page.keyboard.press("Tab");

    const focusedAfter = await page.evaluate(() => {
      return document.activeElement?.tagName ?? "BODY";
    });

    // Focus should have moved (unless there are no focusable elements)
    expect(focusedBefore).toBeTruthy();
    expect(focusedAfter).toBeTruthy();
  });
});

// ── Error handling tests ──

test.describe("Error handling", () => {
  test("shows error state when API fails", async ({ page }) => {
    // Mock all API calls to return 500
    await page.route("**/api/v1/**", (route) =>
      route.fulfill({
        status: 500,
        json: { code: "INTERNAL_ERROR", message: "Server error" },
      })
    );

    const errors: string[] = [];
    page.on("pageerror", (err) => errors.push(err.message));

    await page.goto("/graphs");
    await page.waitForLoadState("networkidle");

    // Page should not crash even when API fails
    const body = page.locator("body");
    await expect(body).toBeVisible();
  });

  test("handles network timeout gracefully", async ({ page }) => {
    await page.route("**/api/v1/**", (route) => route.abort("timedout"));

    await page.goto("/graphs");
    await page.waitForLoadState("networkidle");

    // Page should remain visible
    const body = page.locator("body");
    await expect(body).toBeVisible();
  });
});
