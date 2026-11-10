import { defineConfig, devices } from "@playwright/test";

/**
 * CE-02: Playwright E2E test configuration for AgentForge Studio.
 *
 * Tests run against the Vite dev server (port 3000).
 * The dev server proxies /api/* to http://localhost:8080 (backend).
 *
 * Run E2E tests:
 *   pnpm --filter @agentforge/studio e2e
 *   pnpm --filter @agentforge/studio e2e:ui
 */
export default defineConfig({
  testDir: "./e2e",
  fullyParallel: true,
  forbidOnly: !!process.env.CI,
  retries: process.env.CI ? 2 : 0,
  workers: process.env.CI ? 1 : undefined,
  reporter: "html",
  use: {
    baseURL: "http://localhost:3000",
    trace: "on-first-retry",
    screenshot: "only-on-failure",
  },
  projects: [
    {
      name: "chromium",
      use: { ...devices["Desktop Chrome"] },
    },
  ],
  webServer: {
    command: "pnpm dev",
    url: "http://localhost:3000",
    reuseExistingServer: !process.env.CI,
    timeout: 30_000,
  },
});
